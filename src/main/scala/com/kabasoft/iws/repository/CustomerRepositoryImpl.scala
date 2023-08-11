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


  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id  in Ids).fold(Expr.literal(true))(_ && _)

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

  private def buildInsertQuery(customers: List[Customer]) =
    insertInto(customer)(
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
  ).values(customers.map(toTuple))


  override def create2(c: Customer): ZIO[Any, RepositoryError, Unit]                  = {
    val query = buildInsertQuery(List(c))
    ZIO.logDebug(s"Query to insert Customer is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }

  override def create(c: Customer): ZIO[Any, RepositoryError, Customer] =
    create2(c) *>getBy((c.id, c.company))

  override def create2(models: List[Customer]): ZIO[Any, RepositoryError, Int]        = {
    val query = buildInsertQuery(models)
    ZIO.logDebug(s"Query to insert CustomerXX is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }

  override def create(models: List[Customer]): ZIO[Any, RepositoryError, List[Customer]]        =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Customer])
    }  else {
    create2(models)*>getBy(models.map(_.id), models.head.company)
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

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Customer] = {
    val selectAll = SELECT.where(whereClause(id._1, id._2))
    ZIO.logDebug(s"Query to execute getBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Customer](c => Customer.apply(c)))
        .findFirst(driverLayer, id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Customer]] = for {
    customers <- getBy_(ids, company).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(company).runCollect.map(_.toList)
  }yield customers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))
  def getBy_(ids: List[String], company:String): ZStream[Any, RepositoryError, Customer] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    //ZIO.logDebug(s"Query to execute getBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Customer](c => Customer.apply(c)))
        .provideDriver(driverLayer)
  }
  override def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, Customer]        = {
    val selectAll = SELECT2.where((iban_ === Iban) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByIban is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Customer](c => Customer.apply(c)))
        .findFirst(driverLayer, Iban)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Customer]] = for {
    customers <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(id._2).runCollect.map(_.toList)
  } yield customers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Customer] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(c => Customer.apply(c)))
        .provideDriver(driverLayer)
  }
}
object CustomerRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CustomerRepository] =
    ZLayer.fromFunction(new CustomerRepositoryImpl(_))
}
