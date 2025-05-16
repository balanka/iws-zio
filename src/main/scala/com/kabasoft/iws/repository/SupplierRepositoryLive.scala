package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Supplier}
import java.time.{Instant, LocalDateTime, ZoneId}

final case class SupplierRepositoryLive(postgres: Resource[Task, Session[Task]]
                                        , bankAccRepo:BankAccountRepository) extends SupplierRepository, MasterfileCRUD:
  import SupplierRepositorySQL._

  def transact(s: Session[Task], newCustomers: List[Supplier]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciCustomer =>
        s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
          tryExec(xa, pciCustomer, pciBankAcc, newCustomers, newCustomers.flatMap(_.bankaccounts))

  def transactM(s: Session[Task], models: List[Supplier], bankAccounts: List[BankAccount]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciCustomer =>
        s.prepareR(SupplierRepositorySQL.UPDATE).use: pcuCustomer =>
          s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
            s.prepareR(BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT).use: pcuBankAcc =>
              tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, List.empty
                , bankAccounts, models.map(Supplier.encodeIt2)
                , List.empty)

  def transact(s: Session[Task], newCustomers: List[Supplier], newbankAccount: List[BankAccount], oldCustomers: List[Supplier]
               , oldbankAcc2Update: List[BankAccount], bankAcc2Delete: List[BankAccount]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciCustomer =>
        s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
          s.prepareR(SupplierRepositorySQL.UPDATE).use: pcuCustomer =>
            s.prepareR(BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT).use: pcuBankAcc =>
              s.prepareR(BankAccountRepositorySQL.DELETE_BANK_ACCOUNT).use: pcdBankAcc =>
                tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, pcdBankAcc
                  , newCustomers, newbankAccount
                  , oldCustomers.map(Supplier.encodeIt2), oldbankAcc2Update.map(BankAccount.encodeIt2)
                  , bankAcc2Delete.map(BankAccount.encodeIt3))

  def transact(s: Session[Task], newCustomers: List[Supplier], oldCustomers: List[Supplier]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciCustomer =>
        s.prepareR(SupplierRepositorySQL.UPDATE).use: pcuCustomer =>
          s.prepareR(BankAccountRepositorySQL.insert).use: pciBankAcc =>
            s.prepareR(BankAccountRepositorySQL.UPDATE_BANK_ACCOUNT).use: pcuBankAcc =>
              tryExec(xa, pciCustomer, pciBankAcc, pcuCustomer, pcuBankAcc, newCustomers
                , newCustomers.flatMap(_.bankaccounts), oldCustomers.map(Supplier.encodeIt2)
                , oldCustomers.flatMap(_.bankaccounts).map(BankAccount.encodeIt2))

  override def create(c: Supplier): ZIO[Any, RepositoryError, Int] = create(List(c))
  override def create(models: List[Supplier]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, models))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.bankaccounts).size + models.size)

  override def modify(model: Supplier): ZIO[Any, RepositoryError, Int] = modify(List(model))

  override def modify(models: List[Supplier]): ZIO[Any, RepositoryError, Int] ={
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
  override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Supplier]] = 
    for 
      suppliers     <- list(Id).map(_.toList)
      bankAccounts_ <- bankAccRepo.bankAccout4All(BankAccount.MODEL_ID)
    yield suppliers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  def list(p: (Int, String)): ZIO[Any, RepositoryError, List[Supplier]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Supplier] = queryWithTxUnique(postgres, p, BY_ID)
  override def getByIban(p: (String, Int, String)):ZIO[Any, RepositoryError, Supplier] =queryWithTxUnique(postgres, p, BY_IBAN)
  override def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Supplier]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(p: (List[String], Int, String)): ZIO[Any, RepositoryError, Int] = 
    executeWithTx(postgres, p, DELETE_ALL(p._1.size), p._1.size)
      .mapBoth(e => e, _ => p._1.size)
     //p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)

object SupplierRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & BankAccountRepository, RepositoryError, SupplierRepository] =
    ZLayer.fromFunction(new SupplierRepositoryLive(_, _))

private[repository] object SupplierRepositorySQL:
  
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *:varchar *: varchar *: varchar *: varchar *: varchar *: varchar *:varchar *:varchar *: int4 *:timestamp *: timestamp *: timestamp)
    
  val mfDecoder: Decoder[Supplier] = mfCodec.map:
    case (id, name, description, street, zip, city, state, country, phone, email, account, oaccount, taxCode, vatCode, currency, company, modelid, enterdate, changedate, postingdate) =>
      Supplier(id, name, description, street, zip, city, state, country, phone, email, account, oaccount, taxCode , vatCode
        , currency, company, modelid, toInstant(enterdate), toInstant(changedate), toInstant(postingdate), List.empty[BankAccount])

  val mfEncoder: Encoder[Supplier] = mfCodec.values.contramap(Supplier.encodeIt)


  def base =
    sql""" SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
             , vatCode, currency, company, modelid, enterdate, changedate,postingdate
             FROM   supplier ORDER BY id ASC"""

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Supplier] =
    sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account,  oaccount, tax_code
             , vatCode, currency, company, modelid, enterdate, changedate, postingdate
             FROM   supplier
             WHERE id  IN ( ${varchar.list(nr)} ) AND  modelid = $int4 AND company = $varchar
             ORDER BY id ASC""".query(mfDecoder)

  val BY_IBAN: Query[String *: Int *: String *: EmptyTuple, Supplier] =
    sql"""SELECT su.id, su.name, su.description, su.street, su.zip, su.city, su.state, su.country, su.phone
            , su.email, su.account, su.oaccount, su.tax_code, su.vatcode, su.currency, su.company
             , su.modelid, su.enterdate, su.changedate, su.postingdate
                 FROM   supplier su, bankaccount bankAcc
                 WHERE su.id = bankAcc.owner AND bankAcc.id = $varchar AND
                  su.modelid = $int4 AND su.company = $varchar ORDER BY id ASC""".query(mfDecoder)
  
  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Supplier] =
    sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
          , vatCode, currency, company, modelid, enterdate, changedate, postingdate
             FROM   supplier
             WHERE id = $varchar AND modelid = $int4 AND company = $varchar
             ORDER BY id ASC""".query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Supplier] =
    sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, account, oaccount, tax_code
          , vatCode, currency, company, modelid, enterdate, changedate, postingdate
             FROM   supplier
             WHERE  modelid = $int4 AND company = $varchar
             ORDER BY id ASC""".query(mfDecoder)

  val insert: Command[Supplier] = 
    sql"""INSERT INTO supplier (id, name, description, street, zip, city, state, country, phone, email, account
                            , oaccount, tax_code, vatCode, currency, company, modelid, enterdate, changedate, postingdate)
                                VALUES $mfEncoder """.stripMargin.command

  def insertAll(n: Int): Command[List[Supplier.TYPE2]] = 
    sql"""INSERT INTO supplier (id, name, description, street, zip, city, state, country, phone, email, account
          , oaccount, tax_code, vatCode, currency, company, modelid, enterdate, changedate, postingdate)
          VALUES ${mfCodec.values.list(n)}""".stripMargin.command

  val UPDATE: Command[Supplier.TYPE3] =
    sql"""UPDATE supplier SET name= $varchar, description= $varchar, street= $varchar, zip= $varchar, city= $varchar
          , state= $varchar, country= $varchar, phone= $varchar, email= $varchar, account= $varchar, oaccount= $varchar
          , tax_code = $varchar , vatcode= $varchar, currency=$varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM supplier WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
    
  def DELETE_ALL (nr:Int) : Command[(List[String], Int, String)] =
    sql"DELETE FROM supplier WHERE id  IN ( ${varchar.list(nr)} )  AND modelid = $int4 AND company = $varchar".command