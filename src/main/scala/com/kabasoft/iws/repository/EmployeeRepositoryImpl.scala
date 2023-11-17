package com.kabasoft.iws.repository
import com.kabasoft.iws.repository.Schema.{bankAccountSchema, employee_Schema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Employee, Employee_}
import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

import scala.annotation.nowarn

final class EmployeeRepositoryImpl(pool: ConnectionPool) extends EmployeeRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val employee                                = defineTable[Employee_]("employee")
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
    vatcode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
    ) = employee.columns

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
    vatcode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
  ).from(employee)
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
    vatcode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
  ).from(employee.join(bankAccount).on(id === owner))

  def toTuple(c: Employee)                                                           = (
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

  private def buildInsertQuery(employees: List[Employee]) =
    insertInto(employee)(
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
      modelid,
      enterdate,
      changedate,
      postingdate
    ).values(employees.map(toTuple))


  override def create2(c: Employee): ZIO[Any, RepositoryError, Unit]                  = {
    val query = buildInsertQuery(List(c))
    ZIO.logDebug(s"Query to insert Employee is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }

  private def buildDeleteBankAccount(ids : List[String]): List[Delete[BankAccount]] =
    ids.map(id=>deleteFrom(bankAccount).where(id_ === id))

  override def create2(models: List[Employee]): ZIO[Any, RepositoryError, Int]        = {
    val query = buildInsertQuery(models)
    ZIO.logDebug(s"Query to insert Employee is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }

  override def create(c: Employee): ZIO[Any, RepositoryError, Employee] =
    create2(c) *> getBy((c.id, c.company))

  override def create(models: List[Employee]): ZIO[Any, RepositoryError, List[Employee]]        =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Employee])
    }  else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }


  override def delete(id: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(employee).where(whereClause(id, companyId))
    ZIO.logDebug(s"Delete Employee is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  def splitBankAccounts(s: Employee, persistentBankAccounts: List[BankAccount], flag: Boolean): List[BankAccount] =
    for {
      bankAccounts <- if (flag) s.bankaccounts.filter(ba => persistentBankAccounts.map(_.id).contains(ba.id))
      else s.bankaccounts.filterNot(ba => persistentBankAccounts.map(_.id).contains(ba.id))
    } yield bankAccounts

  private def buildUpdate(model: Employee_): Update[Employee_] =
    update(employee)
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
  override def modify(model: Employee): ZIO[Any, RepositoryError, Int] = {
    val result = for {
      persistentBankAccounts <- getBankAccounts4Employee(model.id, model.company)
      oldBankAccounts = splitBankAccounts(model, persistentBankAccounts, true)
      newBankAccounts = splitBankAccounts(model, persistentBankAccounts, false)
      deleteBankAccounts = model.bankaccounts.filter(_.modelid == -1).map(_.id)
      update_ = buildUpdate(Employee_(model))
      insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(buildInsertBankAccount(newBankAccounts).run)<*
        ZIO.logInfo(s"New bank accounts insert stmt ${renderInsert(buildInsertBankAccount(newBankAccounts))}")
      updatedBankAccounts <- ZIO.when(oldBankAccounts.nonEmpty)(oldBankAccounts.map(ba => buildUpdateBankAccount(ba).run).flip.map(_.sum))<*
        ZIO.logInfo(s"bank accounts to update ${renderUpdate(update_)}")
      deletedBankAccounts <- ZIO.when(deleteBankAccounts.nonEmpty)(buildDeleteBankAccount(deleteBankAccounts).map(_.run).flip.map(_.sum))<*
        ZIO.logInfo(s"bank accounts to delete ${buildDeleteBankAccount(deleteBankAccounts).map(renderDelete)}")
      updated <- update_.run
    } yield insertedBankAccounts.getOrElse(0) + updatedBankAccounts.getOrElse(0) + deletedBankAccounts.getOrElse(0) + updated
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }

  def listBankAccount(companyId: String): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT.where(company_ === companyId)
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }

  def getBankAccounts4Employee(Id: String, companyId: String): ZIO[Any, RepositoryError, List[BankAccount]] = {
    val selectAll = SELECT_BANK_ACCOUNT.where((owner === Id) && (company_ === companyId))
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer).runCollect.map(_.toList)
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Employee]]     = for {
    employees     <- list(companyId).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(companyId).runCollect.map(_.toList)
  } yield employees.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def list(companyId: String): ZStream[Any, RepositoryError, Employee] = {
    val selectAll = SELECT.where(company === companyId)
    execute(selectAll.to(c => Employee.apply(c)))
      .provideDriver(driverLayer)
  }

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Employee] = {
    val selectAll = SELECT.where(whereClause(id._1, id._2))
    ZIO.logDebug(s"Query to execute getBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Employee](c => Employee.apply(c)))
        .findFirst(driverLayer, id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Employee]] = for {
    employees <- getBy_(ids, company).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(company).runCollect.map(_.toList)
  }yield employees.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))
  def getBy_(ids: List[String], company:String): ZStream[Any, RepositoryError, Employee] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    //ZIO.logDebug(s"Query to execute getBy is ${renderRead(selectAll)}") *>
    execute(selectAll.to[Employee](c => Employee.apply(c)))
      .provideDriver(driverLayer)
  }
  override def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, Employee]        = {
    val selectAll = SELECT2.where((id_ === Iban) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByIban is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Employee](c => Employee.apply(c)))
        .findFirst(driverLayer, Iban)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Employee]] = for {
    employees <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(id._2).runCollect.map(_.toList)
  } yield employees.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Employee] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(c => Employee.apply(c)))
        .provideDriver(driverLayer)
  }
}
object EmployeeRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, EmployeeRepository] =
    ZLayer.fromFunction(new EmployeeRepositoryImpl(_))
}
