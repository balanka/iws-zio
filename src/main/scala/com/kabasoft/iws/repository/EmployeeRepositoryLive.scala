package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.{Account, BankAccount, Employee, EmployeeSalaryItem, EmployeeSalaryItemDTO, SalaryItem}
import com.kabasoft.iws.domain.AppError.RepositoryError

import java.time.{Instant, LocalDateTime, ZoneId}

final case class EmployeeRepositoryLive(postgres: Resource[Task, Session[Task]]
                                        , bankAccRepo:BankAccountRepository
                                        , accRepo:AccountRepository) extends EmployeeRepository, MasterfileCRUD:
    import EmployeeRepositorySQL._

    override def create(c: Employee, flag: Boolean):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, if (flag) upsert else insert, 1)

    override def create(list: List[Employee]):ZIO[Any, RepositoryError, Int] =
      executeWithTx(postgres, list.map(Employee.encodeIt), insertAll(list.size), list.size)
    override def modify(model: Employee):ZIO[Any, RepositoryError, Int]= create(model, true)
    override def modify(models: List[Employee]):ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
    def listSalaryItem(p:String): ZIO[Any, RepositoryError, List[EmployeeSalaryItem]] =  queryWithTx(postgres, p, EMPLOYEE_SALARY_ITEM)
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
  
    type S_TYPE =(String, String, String, BigDecimal, BigDecimal, String, String)
    
    private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
      localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

    private val mfCodec =
      (varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *:varchar *: varchar *:
        varchar *: varchar *: varchar *: varchar *: numeric(12, 2) *: int4 *:timestamp *: timestamp *: timestamp)
      
    private val salaryItemCodec = 
      (varchar *: varchar *: varchar *: numeric(12, 2) *: numeric(12, 2) *: varchar *: varchar)

    private[repository]   def encodeSalaryItem(st: EmployeeSalaryItem): S_TYPE =
          (st.id, st.owner, st.account, st.amount, st.percentage, st.text, st.company)
  
    val mfDecoder: Decoder[Employee] = mfCodec.map:
      case (id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
               , salary, modelid, enterdate, changedate, postingdate) =>
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
          vatcode,
          company,
          salary.bigDecimal,
          modelid,
          toInstant(enterdate),
          toInstant(changedate),
          toInstant(postingdate))

    val mfEncoder: Encoder[Employee] = mfCodec.values.contramap(Employee.encodeIt)

    val salaryItemDecoder: Decoder[EmployeeSalaryItem] = salaryItemCodec.map:
      case (id, owner, account, amount, percentage, text, company) =>
        EmployeeSalaryItem(id, owner, account, amount.bigDecimal, percentage.bigDecimal, text, company)
    

    val salaryItemEncoder: Encoder[EmployeeSalaryItem] = salaryItemCodec.values.contramap(encodeSalaryItem)

    val base =
      sql""" SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
               , salary, modelid, enterdate, changedate, postingdate
             FROM   employee """

    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Employee] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
               , salary, modelid, enterdate, changedate, postingdate
             FROM   employee
             WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Employee] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
               , salary, modelid, enterdate, changedate, postingdate
             FROM   employee
             WHERE id = $varchar AND modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val ALL: Query[Int *: String *: EmptyTuple, Employee] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
               , salary, modelid, enterdate, changedate, postingdate
             FROM   employee
             WHERE  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)
      
      
    val EMPLOYEE_SALARY_ITEM: Query[String, EmployeeSalaryItem]  =
     sql""" select id, owner, account, amount, percentage, text, company 
            FROM   employee_salary_item
             WHERE  company = $varchar
          """.query(salaryItemDecoder)

    val insert: Command[Employee] = sql"INSERT INTO employee VALUES $mfEncoder".command
    def insertAll(n: Int): Command[List[Employee.TYPE2]] = sql"INSERT INTO employee VALUES ${mfCodec.values.list(n)}".command

    val insertSalaryItem: Command[EmployeeSalaryItem] = 
      sql"INSERT INTO employee_salary_item VALUES $salaryItemEncoder".command
      
    def insertAllSalaryItem(n: Int): Command[List[S_TYPE]] = 
      sql"INSERT INTO employee_salary_item VALUES ${salaryItemCodec.values.list(n)}".command


    val upsert: Command[Employee] =
      sql"""INSERT INTO employee
             VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
             id                    = EXCLUDED.id,
             name                  = EXCLUDED.name,
             description           = EXCLUDED.description,
              account              = EXCLUDED.account,
              enterdate            = EXCLUDED.enterdate,
              changedate           = EXCLUDED.changedate,
              postingdate          = EXCLUDED.postingdate,
              company              = EXCLUDED.company,
              modelid              = EXCLUDED.modelid,
            """.command

    def DELETE: Command[(String, Int, String)] =
      sql"DELETE FROM employee WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command