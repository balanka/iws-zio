package com.kabasoft.iws.repository

import zio._
import zio.stream._
import zio.sql.ConnectionPool
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError._

final class CustomerRepositoryImpl(pool: ConnectionPool) extends CustomerRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val bankAccount =(string("iban")++string("owner")).table("bankaccount")
  val customer =
    (string("id") ++ string("name") ++ string("description") ++ string("street") ++ string("zip") ++ string("city") ++ string("state") ++ string(
      "country"
    ) ++ string("phone") ++ string("email") ++ string("account") ++ string("revenue_account") ++ string("iban") ++ string("vatcode") ++ string(
      "company"
    ) ++ int("modelid") ++ instant("enter_date") ++ instant("modified_date") ++ instant("posting_date")).table("customer")
  val (iban_, owner) = bankAccount.columns
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
  ) = customer.columns
  val X        =
    id ++ name ++ description ++ street ++ zip ++ city ++ state ++ country ++ phone ++ email ++ account ++ oaccount ++ iban ++ vatcode ++ company ++ modelid ++ enterdate ++ changedate ++ postingdate
  val X2 =
    id ++ name ++ description ++ street ++ zip ++ city ++ state ++ country ++ phone ++ email ++ account ++ oaccount ++ iban_ ++ vatcode ++ company ++ modelid ++ enterdate ++ changedate ++ postingdate
  val SELECT   = select(X).from(customer)
  val SELECT2   = select(X2).from(customer.join(bankAccount).on(id === owner))

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
  override def create(c: Customer): ZIO[Any, RepositoryError, Unit]                    = {
    val query = insertInto(customer)(X).values(toTuple(c))

    ZIO.logDebug(s"Query to insert Customer is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Customer]): ZIO[Any, RepositoryError, Int]          = {
    val data  = models.map(toTuple(_)) // Customer.unapply(_).get)
    val query = insertInto(customer)(X).values(data)

    ZIO.logDebug(s"Query to insert CustomerXX is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(customer).where((id === item.toLong) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Customer): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(customer)
      .set(name, model.name)
      .set(description, model.description)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query Update Customer is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, Customer]              = {
    val selectAll = SELECT.where(company === companyId)
    execute(selectAll.to(c => Customer.apply(c)))
    .provideDriver(driverLayer)
  }

  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Customer] = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))
    ZIO.logDebug(s"Query to execute getBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Customer](c => Customer.apply(c)))
        .findFirst(driverLayer, Id)
  }


  override def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, Customer]        = {
    val selectAll = SELECT2.where((iban_ === Iban) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByIban is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Customer](c => Customer.apply(c)))
        .findFirst(driverLayer, Iban)
  }
  override def getByModelId(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Customer] = {
     val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    execute(selectAll.to[Customer](c => Customer.apply(c)))
      .provideDriver(driverLayer)
  }

}
object CustomerRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CustomerRepository] =
    ZLayer.fromFunction(new CustomerRepositoryImpl(_))
}
