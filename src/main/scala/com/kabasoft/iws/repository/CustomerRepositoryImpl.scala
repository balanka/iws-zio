package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.Schema.{bankAccountSchema, customer_Schema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Customer, Customer_}
import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

import scala.annotation.nowarn

final class CustomerRepositoryImpl(pool: ConnectionPool) extends CustomerRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val customer                                = defineTable[Customer_]("customer")
  val bankAccount                             = defineTable[BankAccount]("bankaccount")
  val (id_, bic, owner, company_, modelid_) = bankAccount.columns


  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id  in Ids).fold(Expr.literal(true))(_ && _)

  def whereClause(Idx: String, companyId: String) =
    List(company === companyId, id === Idx ).fold(Expr.literal(true))(_ && _)

  val SELECT_BANK_ACCOUNT = select(id_, bic, owner, company_, modelid_).from(bankAccount)
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
    //iban,
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
    //iban,
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
    //id_,
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
    //c.iban,
    c.vatcode,
    c.company,
    c.modelid,
    c.enterdate,
    c.changedate,
    c.postingdate
  )

  private def buildInsertBankAccount(ba: List[BankAccount]) =
    insertInto(bankAccount)(id_, bic, owner, company_, modelid_).values(ba.map(BankAccount.unapply(_).get))

  private def buildUpdateBankAccount(model: BankAccount): Update[BankAccount] =
    update(bankAccount)
      .set(bic, model.bic)
      .set(owner, model.owner)
      .set(company_, model.company)
      .where(id_ === model.id)

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
    //iban,
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

  private def buildDeleteBankAccount(ids : List[String]): List[Delete[BankAccount]] =
    ids.map(id=>deleteFrom(bankAccount).where(id_ === id))

  override def create2(models: List[Customer]): ZIO[Any, RepositoryError, Int]        = {
    val query = buildInsertQuery(models)
    ZIO.logDebug(s"Query to insert Customer is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }

  override def create(c: Customer): ZIO[Any, RepositoryError, Customer] =
    create2(c) *> getBy((c.id, c.company))

  override def create(models: List[Customer]): ZIO[Any, RepositoryError, List[Customer]]        =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Customer])
    }  else {
    create2(models)*>getBy(models.map(_.id), models.head.company)
  }


  override def delete(id: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(customer).where(whereClause(id, companyId))
    ZIO.logDebug(s"Delete customer is ${renderDelete(delete_)}") *>
    execute(delete_)
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))
  }
  private def buildUpdate(model: Customer_): Update[Customer_] =
    update(customer)
      .set(name, model.name)
      .set(description, model.description)
      .set(street, model.street)
      .set(zip, model.zip)
      .set(city, model.city)
      .set(state, model.state)
      .set(country, model.country)
      .set(phone, model.phone)
      .set(email, model.email)
      .set(account, model.account)
      .set(oaccount, model.oaccount)
      .set(vatcode, model.vatcode)
      .where(whereClause(model.id, model.company))


  @nowarn
  override def modify(model: Customer): ZIO[Any, RepositoryError, Int] = {
    val oldBankAccounts = model.bankaccounts.filter(_.modelid == -2).map(_.copy(modelid = 12))
    val newBankAccounts = model.bankaccounts.filter(_.modelid == -3).map(_.copy(modelid = 12))
    val deleteBankAccounts = model.bankaccounts.filter(_.modelid == -1).map(_.id)
    val update_ = buildUpdate(Customer_(model))
    val result = for {
      insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(buildInsertBankAccount(newBankAccounts).run)<*
        ZIO.logInfo(s"bank accounts insert stmt ${renderInsert(buildInsertBankAccount(newBankAccounts))}")
      updatedBankAccounts <- ZIO.when(oldBankAccounts.nonEmpty)(oldBankAccounts.map(ba => buildUpdateBankAccount(ba).run).flip.map(_.sum))<*
        ZIO.logInfo(s" bank accounts  update stmt ${oldBankAccounts.map(ba => renderUpdate(buildUpdateBankAccount(ba)))}")
      deletedBankAccounts <- ZIO.when(deleteBankAccounts.nonEmpty)(buildDeleteBankAccount(deleteBankAccounts).map(_.run).flip.map(_.sum))<*
        ZIO.logInfo(s"bank accounts  delete stmt ${buildDeleteBankAccount(deleteBankAccounts).map(renderDelete)}")
      updated <- update_.run <* ZIO.logInfo(s"customer update stmt ${renderUpdate(update_)}")
    } yield insertedBankAccounts.getOrElse(0) + updatedBankAccounts.getOrElse(0) + deletedBankAccounts.getOrElse(0) + updated
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }

  def listBankAccount(companyId: String): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT.where(company_ === companyId)
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }

  def getBankAccounts4Customer(Id: String, companyId: String): ZIO[Any, RepositoryError, List[BankAccount]] = {
    val selectAll = SELECT_BANK_ACCOUNT.where((owner === Id) && (company_ === companyId))
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer).runCollect.map(_.toList)
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
    val selectAll = SELECT2.where((id_ === Iban) && (company === companyId))

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
