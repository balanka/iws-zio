package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain._
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class SupplierRepositoryImpl(pool: ConnectionPool) extends SupplierRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val bankAccount =(string("iban") ++ string("owner")).table("bankaccount")
  val supplier = (string("id") ++ string("name") ++ string("description") ++ string("street")
    ++ string("city") ++ string("state") ++ string("zip") ++ string("country")
    ++ string("phone") ++ string("email") ++ string("account") ++ string("charge_account") ++ string("iban")
    ++ string("vatcode") ++ string("company") ++ int("modelid") ++ instant("enter_date") ++ instant("modified_date")
    ++ instant("posting_date"))
    .table("supplier")
  val (iban_, owner ) = bankAccount.columns
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
  val X        =
    id ++ name ++ description ++ street ++ zip ++ city ++ state ++ country ++ phone ++ email ++ account ++ oaccount ++ iban ++ vatcode ++ company ++ modelid ++ enterdate ++ changedate ++ postingdate
  val X2 =
    id ++ name ++ description ++ street ++ zip ++ city ++ state ++ country ++ phone ++ email ++ account ++ oaccount ++ iban_ ++ vatcode ++ company ++ modelid ++ enterdate ++ changedate ++ postingdate

  val SELECT   = select(X).from(supplier)
  val SELECT2   = select(X2).from(supplier.join(bankAccount).on(id === owner))
  def toTuple(c: Customer)                                                             = (
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
    val query = insertInto(supplier)(X).values(toTuple(c))

    ZIO.logInfo(s"Query to insert Supplier is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Supplier]): ZIO[Any, RepositoryError, Int]          = {
    val data  = models.map(toTuple(_))
    val query = insertInto(supplier)(X).values(data)

    ZIO.logInfo(s"Query to insert Supplier is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(supplier).where((id === item.toLong) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Supplier): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(supplier)
      .set(name, model.name)
      .set(description, model.description)
      .where((id === model.id) && (company === model.company))
    ZIO.logInfo(s"Query Update supplier is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, Supplier]          = {
    val selectAll = SELECT
    execute(selectAll.to[Supplier](c => Supplier.apply(c)))
      .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Supplier] = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirst(driverLayer, Id)
  }

  override def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, Supplier]    = {
    val selectAll = SELECT2.where((iban_ === Iban) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByIban is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirst(driverLayer, Iban)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Supplier] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirstInt(driverLayer, modelId)
  }

}
object SupplierRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, SupplierRepository] =
    ZLayer.fromFunction(new SupplierRepositoryImpl(_))
}
