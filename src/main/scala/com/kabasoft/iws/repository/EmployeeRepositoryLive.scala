package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.{Account, BankAccount, Employee, EmployeeSalaryItem, EmployeeSalaryItemDTO, SalaryItem}
import com.kabasoft.iws.domain.AppError.RepositoryError

import java.time.{Instant, LocalDateTime, ZoneId}

final case class EmployeeRepositoryLive(postgres: Resource[Task, Session[Task]]
                                        , bankAccRepo:BankAccountRepository
                                        , accRepo:AccountRepository) extends EmployeeRepository, MasterfileCRUD:
    import EmployeeRepositorySQL._

     def tryExecX[A, B, C, D, E, F](xa: Transaction[Task], pciEmployee: PreparedCommand[Task, A]
                        , pciBankAcc: PreparedCommand[Task, B]
                        , pciSalaryItem: PreparedCommand[Task, E]
                        , pcuEmployee: PreparedCommand[Task, C]
                        , pcuBankAcc: PreparedCommand[Task, D]
                        , pcuSalaryItem: PreparedCommand[Task, F]
                        , newEmployee: List[A], newBankaccounts: List[B], newSalaryItems:List[E]
                        , oldEmployee: List[C], oldBankaccounts: List[D], oldSalaryItems:List[F]): Task[Unit] =

      for
        sp <- xa.savepoint
        _ <- exec(pciEmployee, newEmployee) *>
             exec(pciBankAcc, newBankaccounts) *>
             exec(pcuEmployee, oldEmployee) *>
             exec(pcuBankAcc, oldBankaccounts) *>
             exec(pciSalaryItem, newSalaryItems) *>
             exec(pcuSalaryItem, oldSalaryItems) 
             .handleErrorWith(ex =>
                ZIO.logInfo(s"Unique violation: ${ex.getMessage}, rolling back...") *>
                xa.rollback(sp))
      yield ()
  
     def transactM(s: Session[Task]
                   , newEmployee: List[Employee]
                   , newBankAccounts: List[BankAccount]
                   , newSalaryItems: List[EmployeeSalaryItem]
                   , oldEmployee: List[Employee]
                   , oldBankAccounts: List[BankAccount]
                   , oldSalaryItems: List[EmployeeSalaryItem]
                  ): Task[Unit] =
       s.transaction.use: xa =>
         s.prepareR(insert).use: pciEmployee =>
           s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
             s.prepareR(insertSalaryItem).use: pciSalaryItem =>
               s.prepareR(EmployeeRepositorySQL.UPDATE).use: pcuEmployee =>
                s.prepareR(BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT).use: pcuBankAcc =>
                   s.prepareR(UPDATE_SALARY_ITEM).use: pcuSalaryItem =>              
                     tryExecX(xa, pciEmployee, pciBankAcc, pciSalaryItem, pcuEmployee, pcuBankAcc, pcuSalaryItem
                       , newEmployee, newBankAccounts, newSalaryItems, oldEmployee.map(Employee.encodeIt2)
                       , oldBankAccounts.map(BankAccount.encodeIt2), oldSalaryItems.map(EmployeeSalaryItem.encodeIt))
            

    override def create(c: Employee):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, insert, 1)

    override def create(list: List[Employee]):ZIO[Any, RepositoryError, Int] =
      executeWithTx(postgres, list.map(Employee.encodeIt), insertAll(list.size), list.size)

    override def modify(model: Employee): ZIO[Any, RepositoryError, Int] = modify(List(model))

    override def modify(modelsx: List[Employee]): ZIO[Any, RepositoryError, Int] =
      val newBankaccounts = modelsx.flatMap(_.bankaccounts).filter(m => !m.id.isEmpty && m.modelid == - 1)
        .map(m => m.copy(modelid = BankAccount.MODEL_ID))
      val oldBankaccounts = modelsx.flatMap(_.bankaccounts).filter(m => !m.id.isEmpty && m.modelid == -2)
                                 .map(m => m.copy(modelid = BankAccount.MODEL_ID))
      val newSalaryItems = modelsx.flatMap(_.salaryItems.filter(m => !m.id.isEmpty && m.owner.equals("-1")))
      val oldSalaryItems = modelsx.flatMap(_.salaryItems.filter(m => !m.id.isEmpty && m.owner.equals("-2")))
      val models: List[Employee] = modelsx.map(e=>e.copy(bankaccounts = newBankaccounts++oldBankaccounts
                                   , salaryItems = (newSalaryItems++oldSalaryItems).map(_.copy(owner = e.id))))
      (postgres
        .use:
           session =>
             transactM(session, List.empty, newBankaccounts, newSalaryItems.map(EmployeeSalaryItem.apply)
               , models, oldBankaccounts, oldSalaryItems.map(EmployeeSalaryItem.apply)))
           .mapBoth(e => RepositoryError(e.getMessage), _ => models.size +
                       models.flatMap(_.bankaccounts).size + models.flatMap(_.salaryItems).size)
  

    private def listSalaryItem(p:String): ZIO[Any, RepositoryError, List[EmployeeSalaryItem]] =  queryWithTx(postgres, p, EMPLOYEE_SALARY_ITEM)
    def list(p: (Int, String)): ZIO[Any, RepositoryError, List[Employee]] =  queryWithTx(postgres, p, ALL)
    
    override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Employee]] = for {
      employee <- list(Id)
      bankAccounts_ <- bankAccRepo.bankAccout4All(BankAccount.MODEL_ID)
      accounts <- accRepo.all((BankAccount.MODEL_ID, Id._2))
      salaryItems_ <- listSalaryItem(Id._2)
    } yield employee.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id),
      salaryItems = salaryItems_.filter(_.id == c.id).map(EmployeeSalaryItemDTO.apply)
        .map(item =>
          item.copy(accountName = accounts.find(acc => acc.id == item.account).getOrElse(Account.dummy).name))))
  
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Employee] = queryWithTxUnique(postgres, p, BY_ID)
    override def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Employee]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
    
object EmployeeRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]]&BankAccountRepository&AccountRepository, RepositoryError, EmployeeRepository] =
    ZLayer.fromFunction(new EmployeeRepositoryLive(_, _, _))

private[repository] object EmployeeRepositorySQL:
  
    private type S_TYPE =(String, String, String, BigDecimal, BigDecimal, String, String)
    
    private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
      localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

    private val mfCodec =
      (varchar *: varchar *: varchar *: varchar *:varchar *: varchar *: varchar *: varchar *: varchar *:varchar *: varchar *:
        varchar *: varchar *: varchar *: varchar *:varchar *: numeric(12, 2) *: int4 *:timestamp *: timestamp *: timestamp)
      
    private val salaryItemCodec = 
      (varchar *: varchar *: varchar *: numeric(12, 2) *: numeric(12, 2) *: varchar *: varchar)

    private[repository]   def encodeSalaryItem(st: EmployeeSalaryItem): S_TYPE =
          (st.id, st.owner, st.account, st.amount, st.percentage, st.text, st.company)
  
    val mfDecoder: Decoder[Employee] = mfCodec.map:
      case (id, name, description, street, zip, city, state, country, phone, email, account, oaccount, taxCode
           , vatcode, currency, company, salary, modelid, enterdate, changedate, postingdate) =>
        Employee(
          id,
          name,
          description,
          street,
          zip,
          city,
          state,
          country,
          phone,
          email,
          account,
          oaccount,
          taxCode,
          vatcode,
          currency,
          company,
          salary.bigDecimal,
          modelid,
          toInstant(enterdate),
          toInstant(changedate),
          toInstant(postingdate))

    val mfEncoder: Encoder[Employee] = mfCodec.values.contramap(Employee.encodeIt)

    private val salaryItemDecoder: Decoder[EmployeeSalaryItem] = salaryItemCodec.map:
      case (id, owner, account, amount, percentage, text, company) =>
        EmployeeSalaryItem(id, owner, account, amount.bigDecimal, percentage.bigDecimal, text, company)
  
    private val salaryItemEncoder: Encoder[EmployeeSalaryItem] = salaryItemCodec.values.contramap(encodeSalaryItem)

    val base =
      sql""" SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
             , vatcode, currency, company, salary, modelid, enterdate, changedate, postingdate
             FROM   employee ORDER BY id ASC"""

    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Employee] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
            , vatcode, currency, company, salary, modelid, enterdate, changedate, postingdate
             FROM   employee
             WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
             ORDER BY id ASC""".query(mfDecoder)

    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Employee] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
            , vatcode, currency, company, salary, modelid, enterdate, changedate, postingdate
             FROM   employee
             WHERE id = $varchar AND modelid = $int4 AND company = $varchar
             ORDER BY id ASC""".query(mfDecoder)

    val ALL: Query[Int *: String *: EmptyTuple, Employee] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
            , vatcode, currency, company, salary, modelid, enterdate, changedate, postingdate
             FROM   employee
             WHERE  modelid = $int4 AND company = $varchar
             ORDER BY id ASC""".query(mfDecoder)
      
      
    val EMPLOYEE_SALARY_ITEM: Query[String, EmployeeSalaryItem]  =
     sql""" select id, owner, account, amount, percentage, text, company 
            FROM   employee_salary_item
             WHERE  company = $varchar
          ORDER BY id ASC""".query(salaryItemDecoder)

    val insert: Command[Employee] = sql"INSERT INTO employee VALUES $mfEncoder".command
    def insertAll(n: Int): Command[List[Employee.TYPE2]] = sql"INSERT INTO employee VALUES ${mfCodec.values.list(n)}".command
//id: String, owner: String, account: String, accountName: String, amount: BigDecimal, percentage: BigDecimal, text:String, company: String
    val insertSalaryItem: Command[EmployeeSalaryItem] = 
      sql"""INSERT INTO employee_salary_item (id, owner, account, amount, text, company, percentage ) 
            VALUES $salaryItemEncoder""".command
      
    def insertAllSalaryItem(n: Int): Command[List[S_TYPE]] = 
      sql"INSERT INTO employee_salary_item VALUES ${salaryItemCodec.values.list(n)}".command

    val UPDATE: Command[Employee.TYPE3] =
       sql"""UPDATE employee SET name= $varchar, description= $varchar, street= $varchar, zip= $varchar, city= $varchar
               , state= $varchar, country= $varchar, phone= $varchar, email= $varchar, account= $varchar, oaccount= $varchar
               , tax_code=$varchar, vatcode= $varchar, currency= $varchar, salary=$numeric
               WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
 
    val UPDATE_SALARY_ITEM: Command[EmployeeSalaryItem.TYPE] =
      sql"""UPDATE employee_salary_item SET owner= $varchar, account= $varchar, amount= $numeric, percentage= $numeric 
            , text= $varchar
               WHERE id=$varchar  and company= $varchar""".command
  
    def DELETE: Command[(String, Int, String)] =
      sql"DELETE FROM employee WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command