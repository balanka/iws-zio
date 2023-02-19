package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{BankAccount, Supplier, Supplier_}
import zio._
import com.kabasoft.iws.repository.Schema.{supplier_Schema, bankAccountSchema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio.sql.ConnectionPool
import zio.stream._

final class SupplierRepositoryImpl(pool: ConnectionPool) extends SupplierRepository with IWSTableDescriptionPostgres {
  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val supplier = defineTable[Supplier_]("supplier")
  val bankAccount = defineTable[BankAccount]("bankaccount")
  val (iban_, bic, owner, company_, modelid_) = bankAccount.columns

  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId).fold(Expr.literal(true))(_ && _)

  val SELECT_BANK_ACCOUNT = select(iban_, bic, owner, company_, modelid_).from(bankAccount)

  val (
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
    iban,
    vatcode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
  ) = supplier.columns

  val SELECT   = select(id, name, description, street, zip, city, state, country, phone, email, account, oaccount, iban,
    vatcode, company, modelid, enterdate, changedate, postingdate)
    .from(supplier)
  val SELECT2   = select(id, name, description, street, zip, city, state, country, phone, email, account, oaccount, iban_
    , vatcode, company, modelid, enterdate, changedate, postingdate)
    .from(supplier.join(bankAccount).on(id === owner))
  def toTuple(c: Supplier)                                                             = (
    c.id,
    c.name,
    c.description,
    c.street,
    c.zip,
    c.city,
    c.state,
    c.country,
    c.phone,
    c.email,
    c.account,
    c.oaccount,
    c.iban,
    c.vatcode,
    c.company,
    c.modelid,
    c.enterdate,
    c.changedate,
    c.postingdate
  )

  override def create(c: Supplier): ZIO[Any, RepositoryError, Unit]                    = {
    val query = insertInto(supplier)(id, name, description, street, zip, city, state, country, phone, email, account, oaccount, iban,
      vatcode, company, modelid, enterdate, changedate, postingdate).values(toTuple(c))

    ZIO.logDebug(s"Query to insert Supplier is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Supplier]): ZIO[Any, RepositoryError, Int]          = {
    val data  = models.map(toTuple(_))
    val query = insertInto(supplier)(id, name, description, street, zip, city, state, country, phone, email, account, oaccount, iban,
      vatcode, company, modelid, enterdate, changedate, postingdate).values(data)

    ZIO.logDebug(s"Query to insert Supplier is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(supplier).where(whereClause(item, companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Supplier): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(supplier)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause(model.id, model.company))
    ZIO.logDebug(s"Query Update supplier is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  def listBankAccount(companyId: String): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT.where(company_ === companyId)
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Supplier]] = for {
    suppliers <- list(companyId).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(companyId).runCollect.map(_.toList)
  } yield suppliers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def list(companyId: String): ZStream[Any, RepositoryError, Supplier]          = {
    val selectAll = SELECT
    execute(selectAll.to[Supplier](c => Supplier.apply(c)))
      .provideDriver(driverLayer)
  }
  override def getBy(id: String, companyId: String): ZIO[Any, RepositoryError, Supplier] = {
    val selectAll = SELECT.where(whereClause(id, companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirst(driverLayer, id)
  }

  override def getByIban(iban: String, companyId: String): ZIO[Any, RepositoryError, Supplier]    = {
    val selectAll = SELECT2.where((iban_ === iban) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByIban is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirst(driverLayer, iban)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Supplier] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirstInt(driverLayer, modelId)
  }

}
object SupplierRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, SupplierRepository] =
    ZLayer.fromFunction(new SupplierRepositoryImpl(_))
}
