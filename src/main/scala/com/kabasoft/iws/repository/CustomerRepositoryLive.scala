package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.interop.catz._
import zio.interop.catz.asyncInstance
import zio.prelude.FlipOps
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Customer}
import java.time.{Instant, LocalDateTime, ZoneId}

final case class CustomerRepositoryLive(postgres: Resource[Task, Session[Task]]
                                        , bankAccRepo:BankAccountRepository) extends CustomerRepository, MasterfileCRUD:
    import CustomerRepositorySQL._

  //  // C= Customer.TYPE3
  //  // D= BankAccount.TYPE2
    def transact(s: Session[Task], newCustomers: List[Customer]): Task[Unit] =
        s.transaction.use: xa =>
          s.prepareR(insert).use: pciCustomer =>
            s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
              tryExec(xa, pciCustomer, pciBankAcc, newCustomers,  newCustomers.flatMap(_.bankaccounts))

    def transactM(s: Session[Task], models: List[Customer], bankAccounts: List[BankAccount]): Task[Unit] =
      s.transaction.use: xa =>
        s.prepareR(insert).use: pciCustomer =>
          s.prepareR(CustomerRepositorySQL.UPDATE).use: pcuCustomer =>
            s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
              s.prepareR(BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT).use: pcuBankAcc =>
                tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, List.empty
                 , bankAccounts, models.map(Customer.encodeIt2), List.empty)

    def transact(s: Session[Task], newCustomers: List[Customer], newbankAccount: List[BankAccount], oldCustomers: List[Customer]
                 , oldbankAcc2Update: List[BankAccount], bankAcc2Delete: List[BankAccount]): Task[Unit] =
      s.transaction.use: xa =>
       s.prepareR(insert).use: pciCustomer =>
         s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
           s.prepareR(CustomerRepositorySQL.UPDATE).use: pcuCustomer =>
            s.prepareR(BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT).use: pcuBankAcc =>
              s.prepareR(BankAccountRepositorySQL.DELETE_BANK_ACCOUNT).use: pcdBankAcc =>
               tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, pcdBankAcc
                 , newCustomers, newbankAccount
                 , oldCustomers.map(Customer.encodeIt2), oldbankAcc2Update.map(BankAccount.encodeIt2)
                , bankAcc2Delete.map(BankAccount.encodeIt3))
            
    def transact(s: Session[Task], newCustomers: List[Customer], oldCustomers: List[Customer]): Task[Unit] =
       s.transaction.use: xa =>
         s.prepareR(insert).use: pciCustomer =>
           s.prepareR(CustomerRepositorySQL.UPDATE).use: pcuCustomer =>
             s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
               s.prepareR(BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT).use: pcuBankAcc =>
                 tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, newCustomers
                   , newCustomers.flatMap(_.bankaccounts), oldCustomers.map(Customer.encodeIt2)
                   , oldCustomers.flatMap(_.bankaccounts).map(BankAccount.encodeIt2))
  
    override def create(c: Customer): ZIO[Any, RepositoryError, Int] = create(List(c))
    override def create(models: List[Customer]):ZIO[Any, RepositoryError, Int] =
      (postgres
        .use:
            session =>
              transact(session, models))
                .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.bankaccounts).size + models.size )
  
    override def modify(model: Customer):ZIO[Any, RepositoryError, Int] = modify(List(model))
     
    override def modify(models: List[Customer]):ZIO[Any, RepositoryError, Int] = {
      val oldLines2Update = models.flatMap(_.bankaccounts).filter(bankAccount => bankAccount.modelid>0 
          && bankAccount.company.contains("-"))
        .map(bankAccount =>bankAccount.copy(company = bankAccount.company.replace("-","")))
      val newLine2Insert = models.flatMap(_.bankaccounts).filter(bankAccount =>bankAccount.modelid === -1 
                                && bankAccount.company.contains("-"))
                                  .map(bankAccount => bankAccount.copy(modelid = BankAccount.MODEL_ID,
                                             company = bankAccount.company.replace("-", "")))
      val oldLine2Delete = models.flatMap(_.bankaccounts).filter(_.modelid === -2)
        .map(bankAccount => bankAccount.copy(company = bankAccount.company.replace("-", "")))
        ZIO.logInfo(s"models ${models}") *>
          ZIO.logInfo(s"oldLines2Update ${oldLines2Update}") *>
          ZIO.logInfo(s"newLine2Insert ${newLine2Insert}")*>
          ZIO.logInfo(s"oldLine2Delete ${oldLine2Delete}")*>
      postgres
        .use:
          session =>
            transact(session, List.empty, newLine2Insert, models, oldLines2Update,  oldLine2Delete)
        .mapBoth(e => RepositoryError(e.getMessage), _ => 
          models.size+newLine2Insert.size+oldLines2Update.size+oldLine2Delete.size)
    }
 
    override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Customer]] = for {
                  customer <- list(Id)
                  bankAccounts_ <- bankAccRepo.bankAccout4All(BankAccount.MODEL_ID)
             } yield customer.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))
    
    private def list(p: (Int, String)): ZIO[Any, RepositoryError, List[Customer]] =  queryWithTx(postgres, p, ALL)
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Customer] = queryWithTxUnique(postgres, p, BY_ID)
    override def getByIban(p: (String, Int, String)): ZIO[Any, RepositoryError, Customer] = queryWithTxUnique(postgres, p, BY_IBAN)
    override def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Customer]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
    override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)

object  CustomerRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & BankAccountRepository, RepositoryError, CustomerRepository] =
    ZLayer.fromFunction(new CustomerRepositoryLive(_, _))

private[repository] object CustomerRepositorySQL:

    private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
      localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

    private val mfCodec =
        (varchar *: varchar *: varchar *: varchar *: varchar*: varchar *: varchar *: varchar *: varchar *:varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: int4 *:timestamp *: timestamp *: timestamp)

    val mfDecoder: Decoder[Customer] = mfCodec.map:
      case (id, name, description, street, zip, city, state, country, phone, email, account, oaccount, taxCode, vatCode, company, modelid, enterdate, changedate, postingdate) =>
        Customer.apply(
          (id, name, description, street, zip, city, state, country, phone, email, account, oaccount, taxCode
            , vatCode, company, modelid, toInstant(enterdate), toInstant(changedate), toInstant(postingdate)))
  
    val mfEncoder: Encoder[Customer] = mfCodec.values.contramap(Customer.encodeIt)
  
    def base =
      sql""" SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
             , vatCode, company, modelid, enterdate, changedate, postingdate
             FROM   customer """

    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
            , vatCode, company, modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE id  IN (${varchar.list(nr)} ) AND  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val BY_IBAN: Query[String *: Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT cu.id, cu.name, cu.description, cu.street, cu.zip, cu.city, cu.state, cu.country, cu.phone
            , cu.email, cu.account, cu.oaccount, cu.vatcode, cu.tax_code, cu.company
             , cu.modelid, cu.enterdate, cu.changedate, cu.postingdate
                 FROM   customer cu, bankaccount bankAcc
                 WHERE cu.id = bankAcc.owner AND bankAcc.id = $varchar AND
                  cu.modelid = $int4 AND cu.company = $varchar """.query(mfDecoder)
  
    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
            , vatCode, company, modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE id = $varchar AND modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val ALL: Query[Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
            , vatCode, company, modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val insert: Command[Customer] =
      sql"""INSERT INTO customer (id, name, description, street, zip, city, state, country, phone, email, account
            , oaccount, tax_code, vatCode, company, modelid, enterdate, changedate, postingdate)
            VALUES $mfEncoder """.command

    def insertAll(n: Int): Command[List[Customer.TYPE2]] =
      sql"""INSERT INTO customer (id, name, description, street, zip, city, state, country, phone, email, account
            , oaccount, tax_code, vatCode, company, modelid, enterdate, changedate, postingdate)
           VALUES ${mfCodec.values.list(n)}""".command

    val UPDATE: Command[Customer.TYPE3] =
      sql"""UPDATE customer SET name= $varchar, description= $varchar, street= $varchar, zip= $varchar, city= $varchar
            , state= $varchar, country= $varchar, phone= $varchar, email= $varchar, account= $varchar, oaccount= $varchar
            , tax_code= $varchar, vatcode= $varchar
            WHERE id=$varchar and modelid=$int4 and company= $varchar""".command

    def DELETE: Command[(String, Int, String)] =
      sql"DELETE FROM customer WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command