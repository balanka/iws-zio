package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
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
              s.prepareR(BankAccountRepositorySQL.UPDATE).use: pcuBankAcc =>
                tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, List.empty
                 , bankAccounts, models.map(Customer.encodeIt2), List.empty)

    def transact(s: Session[Task], newCustomers: List[Customer], oldCustomers: List[Customer]): Task[Unit] =
       s.transaction.use: xa =>
         s.prepareR(insert).use: pciCustomer =>
           s.prepareR(CustomerRepositorySQL.UPDATE).use: pcuCustomer =>
             s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
               s.prepareR(BankAccountRepositorySQL.UPDATE).use: pcuBankAcc =>
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
     
    override def modify(modelsx: List[Customer]):ZIO[Any, RepositoryError, Int] =
      val bankaccountsx = modelsx.flatMap(_.bankaccounts).filter(_.modelid < 0)
                                 .map(m => m.copy(modelid = BankAccount.MODEL_ID))
      val models = modelsx.map(_.copy(bankaccounts = bankaccountsx))
      (postgres
        .use: 
          session => 
            transactM(session, models, bankaccountsx))
             .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.bankaccounts).size + models.size)
      
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
        (varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *:varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: int4 *:timestamp *: timestamp *: timestamp)

    val mfDecoder: Decoder[Customer] = mfCodec.map:
      case (id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company, modelid, enterdate, changedate, postingdate) =>
        Customer.apply(
          (id, name, description, street, zip, city, state, country, phone, email, account, oaccount,
             vatcode, company, modelid, toInstant(enterdate), toInstant(changedate), toInstant(postingdate)))
  
    val mfEncoder: Encoder[Customer] = mfCodec.values.contramap(Customer.encodeIt)
  
    def base =
      sql""" SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer """

    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE id  IN (${varchar.list(nr)} ) AND  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val BY_IBAN: Query[String *: Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT cu.id, cu.name, cu.description, cu.street, cu.zip, cu.city, cu.state, cu.country, cu.phone
            , cu.email, cu.account, cu.oaccount, cu.vatcode, cu.company
             , cu.modelid, cu.enterdate, cu.changedate, cu.postingdate
                 FROM   customer cu, bankaccount bankAcc
                 WHERE cu.id = bankAcc.owner AND bankAcc.id = $varchar AND
                  cu.modelid = $int4 AND cu.company = $varchar """.query(mfDecoder)
  
    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE id = $varchar AND modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val ALL: Query[Int *: String *: EmptyTuple, Customer] =
      sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, vatcode, company
             , modelid, enterdate, changedate, postingdate
             FROM   customer
             WHERE  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val insert: Command[Customer] =
      sql"""INSERT INTO customer (id, name, description, street, zip, city, state, country, phone, email, account
            , oaccount, vatcode, company, modelid, enterdate, changedate, postingdate)
            VALUES $mfEncoder """.command

    def insertAll(n: Int): Command[List[Customer.TYPE2]] =
      sql"""INSERT INTO customer (id, name, description, street, zip, city, state, country, phone, email, account
            , oaccount, vatcode, company, modelid, enterdate, changedate, postingdate)
           VALUES ${mfCodec.values.list(n)}""".command

    val UPDATE: Command[Customer.TYPE3] =
      sql"""UPDATE customer SET name= $varchar, description= $varchar, street= $varchar, zip= $varchar, city= $varchar
            , state= $varchar, country= $varchar, phone= $varchar, email= $varchar, account= $varchar, oaccount= $varchar
            , vatcode= $varchar
            WHERE id=$varchar and modelid=$int4 and company= $varchar""".command

    def DELETE: Command[(String, Int, String)] =
      sql"DELETE FROM customer WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command