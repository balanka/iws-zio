package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.Schema.{ bankAccountSchema, customer_Schema }
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{ BankAccount, Customer, Customer_ }
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class CustomerRepositoryImpl(pool: ConnectionPool) extends CustomerRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val customer                                = defineTable[Customer_]("customer")
  val bankAccount                             = defineTable[BankAccount]("bankaccount")
  val (iban_, bic, owner, company_, modelid_) = bankAccount.columns

  def whereClause(Idx: String, companyId: String) =
    List(company === companyId, id === Idx ).fold(Expr.literal(true))(_ && _)

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
  ) = customer.columns

  val SELECT  = select(
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
  ).from(customer)
  val SELECT2 = select(
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
    iban_,
    vatcode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
  ).from(customer.join(bankAccount).on(id === owner))

  def toTuple(c: Customer)                                                           = (
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
  override def create(c: Customer): ZIO[Any, RepositoryError, Unit]                  = {
    val query = insertInto(customer)(
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
    ).values(toTuple(c))

    ZIO.logDebug(s"Query to insert Customer is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Customer]): ZIO[Any, RepositoryError, Int]        = {
    val data  = models.map(toTuple(_)) // Customer.unapply(_).get)
    val query = insertInto(customer)(
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
    ).values(data)

    ZIO.logDebug(s"Query to insert CustomerXX is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(id: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(customer).where(whereClause(id, companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: Customer): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(customer)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause(model.id, model.company))
    ZIO.logDebug(s"Query Update Customer is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  def listBankAccount(companyId: String): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT.where(company_ === companyId)
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def all(companyId: String): ZIO[Any, RepositoryError, List[Customer]]     = for {
    customers     <- list(companyId).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(companyId).runCollect.map(_.toList)
  } yield customers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def list(companyId: String): ZStream[Any, RepositoryError, Customer] = {
    val selectAll = SELECT.where(company === companyId)
    execute(selectAll.to(c => Customer.apply(c)))
      .provideDriver(driverLayer)
  }

  override def getBy(id: String, companyId: String): ZIO[Any, RepositoryError, Customer] = {
    val selectAll = SELECT.where(whereClause(id, companyId))
    ZIO.logDebug(s"Query to execute getBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Customer](c => Customer.apply(c)))
        .findFirst(driverLayer, id)
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
