package com.kabasoft.iws.repository
import com.kabasoft.iws.repository.Schema.{bankAccountSchema, employee_Schema, employeeSalaryItemSchema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankAccount, Employee, EmployeeSalaryItem, Employee_}
import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

import scala.annotation.nowarn

final class EmployeeRepositoryImpl(pool: ConnectionPool) extends EmployeeRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val employee                                = defineTable[Employee_]("employee")
  val bankAccount                             = defineTable[BankAccount]("bankaccount")
  val salaryItem                             = defineTable[EmployeeSalaryItem]("employee_salary_item")
  val (id_, bic, owner, company_, modelid_) = bankAccount.columns
  val (id2, owner2, account2, amount, text, company2) = salaryItem.columns


  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id  in Ids).fold(Expr.literal(true))(_ && _)

  def whereClause(Idx: String, companyId: String) =
    List(company === companyId, id === Idx ).fold(Expr.literal(true))(_ && _)

  val SELECT_BANK_ACCOUNT = select(id_, bic, owner, company_, modelid_).from(bankAccount)
  val SELECT_SALARY_ITEM = select(id2, owner2, account2, amount, text, company2).from(salaryItem)
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
    salary,
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
    salary,
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
    salary,
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
    c.salary,
    c.modelid,
    c.enterdate,
    c.changedate,
    c.postingdate
  )

  private def buildInsertSalaryItem(ba: List[EmployeeSalaryItem]) =
    insertInto(salaryItem)(id2, owner2, account2, amount, text, company2).values(ba.map(EmployeeSalaryItem.unapply(_).get))
  private def buildInsertBankAccount(ba: List[BankAccount]) =
    insertInto(bankAccount)(id_, bic, owner, company_, modelid_).values(ba.map(BankAccount.unapply(_).get))

  private def buildUpdateSalaryItem(model: EmployeeSalaryItem): Update[EmployeeSalaryItem] =
    update(salaryItem)
      .set(account2, model.account)
      .set(owner2, model.owner)
      .set(amount, model.amount)
      .set(text, model.text)
      .set(company2, model.company)
      .where(id2 === model.id)

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
      salary,
      modelid,
      enterdate,
      changedate,
      postingdate
    ).values(employees.map(toTuple))


  override def create2(c: Employee): ZIO[Any, RepositoryError, Int]                  = buildInsert(List(c))
  private def buildDeleteBankAccount(ids : List[String]): List[Delete[BankAccount]] =
    ids.map(id=>deleteFrom(bankAccount).where(id_ === id))

  private def buildDeleteSalaryItem(id:String, accounts: List[String]): List[Delete[EmployeeSalaryItem]] =
    accounts.map(acc => deleteFrom(salaryItem).where(id2 === id && account2 === acc))

  override def create(c: Employee): ZIO[Any, RepositoryError, Employee] =
    buildInsert(List(c))*> getBy((c.id, c.company))

  override def create(models: List[Employee]): ZIO[Any, RepositoryError, List[Employee]]        =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Employee])
    }  else {
      buildInsert(models)*>getBy(models.map(_.id), models.head.company)
    }
  @nowarn
  override def buildInsert(models: List[Employee]): ZIO[Any, RepositoryError, Int] = {
    var idx = "-1x"
    var companyx = "-1x"
    val newSalaryItems = models.flatMap(_.salaryItems.filter(_.id == -3.toString)
                               .map(s=>{idx = s.id; companyx = s.company; s.copy(id = s.id)}))
    val inserts = buildInsertQuery(models)
    val bankAccountsToInsert = models.flatMap(_.bankaccounts)
    val result = for {
      persistentBankAccounts <- getBankAccounts4Employee(idx, companyx)
      newBankAccounts = splitBankAccounts(bankAccountsToInsert, persistentBankAccounts, false)
      insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(buildInsertBankAccount(newBankAccounts).run)<*
        ZIO.logInfo(s"bank accounts insert stmt ${renderInsert(buildInsertBankAccount(newBankAccounts))}")
      insertedSalaryItems <- ZIO.when(newSalaryItems.nonEmpty)(buildInsertSalaryItem(newSalaryItems).run)<*
        ZIO.logInfo(s"Insert salary item  stmt ${renderInsert(buildInsertSalaryItem(newSalaryItems))}")
      insert <- inserts.run <* ZIO.logInfo(s" Insert Employee  stmt ${renderInsert(inserts)}")
    } yield insertedBankAccounts.getOrElse(0) + insertedSalaryItems.getOrElse(0)  + insert
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }


  override def delete(idx: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(employee).where( (company === companyId) && (id === idx)  )
    ZIO.logDebug(s"Delete Employee is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }
  def splitBankAccounts( bankaccounts: List[BankAccount], persistentBankAccounts: List[BankAccount], flag: Boolean): List[BankAccount] =
    for {
      bankAccounts <- if (flag) bankaccounts.filter(ba => persistentBankAccounts.map(_.id).contains(ba.id))
      else bankaccounts.filterNot(ba => persistentBankAccounts.map(_.id).contains(ba.id))
    } yield bankAccounts

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
      .set(salary,model.salary)
      .where(whereClause(model.id, model.company))


  @nowarn
  override def modify(model: Employee): ZIO[Any, RepositoryError, Int] = {
    val newSalaryItems = model.salaryItems.filter(_.id == -3.toString).map(_.copy(id = model.id))
    val oldSalaryItems = model.salaryItems.filter(_.id == -2.toString).map(_.copy(id = model.id))
    val deleteSalaryItems = model.salaryItems.filter(_.id == -1.toString).map(_.copy(id = model.id))
    val result = for {
      _ <-ZIO.logInfo(s"oldSalaryItems ${oldSalaryItems}")
      persistentBankAccounts <- getBankAccounts4Employee(model.id, model.company)
      oldBankAccounts = splitBankAccounts(model, persistentBankAccounts, true)
      newBankAccounts = splitBankAccounts(model, persistentBankAccounts, false)
      deleteBankAccounts = model.bankaccounts.filter(_.modelid == -1).map(_.id)
      updateSalaryItems = oldSalaryItems.map(ba => buildUpdateSalaryItem(ba))
      update_ = buildUpdate(Employee_(model))
      insertedSalaryItems <- ZIO.when(newSalaryItems.nonEmpty)(buildInsertSalaryItem(newSalaryItems).run)<*
        ZIO.logInfo(s"New salary items insert stmt ${renderInsert(buildInsertSalaryItem(newSalaryItems))}")
      insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(buildInsertBankAccount(newBankAccounts).run)<*
        ZIO.logInfo(s"New bank accounts insert stmt ${renderInsert(buildInsertBankAccount(newBankAccounts))}")
      updatedBankAccounts <- ZIO.when(oldBankAccounts.nonEmpty)(oldBankAccounts.map(ba => buildUpdateBankAccount(ba).run).flip.map(_.sum))<*
        ZIO.logInfo(s"bank accounts to update ${renderUpdate(update_)}")
      updatedSalaryItems <- ZIO.when(oldSalaryItems.nonEmpty)(updateSalaryItems.map(_.run).flip.map(_.sum))<*
        ZIO.logInfo(s"salary items  to update ${ updateSalaryItems.map(renderUpdate)}")
      deletedBankAccounts <- ZIO.when(deleteBankAccounts.nonEmpty)(buildDeleteBankAccount(deleteBankAccounts).map(_.run).flip.map(_.sum))<*
        ZIO.logInfo(s"bank accounts to delete ${buildDeleteBankAccount(deleteBankAccounts).map(renderDelete)}")
      deletedSalaryItems <- ZIO.when(deleteSalaryItems.nonEmpty)(buildDeleteSalaryItem(model.id, deleteSalaryItems.map(_.account)).map(_.run).flip.map(_.sum))<*
        ZIO.logInfo(s"Employee salary items  to delete ${buildDeleteBankAccount(deleteBankAccounts).map(renderDelete)}")
      updated <- update_.run
    } yield insertedSalaryItems.getOrElse(0) +insertedBankAccounts.getOrElse(0) + updatedBankAccounts.getOrElse(0) +
            deletedSalaryItems.getOrElse(0) + deletedBankAccounts.getOrElse(0) + updatedSalaryItems.getOrElse(0) + updated
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }

  def listBankAccount(companyId: String): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT.where(company_ === companyId)
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }

  def listEmployeeSalaryItem(companyId: String): ZStream[Any, RepositoryError, EmployeeSalaryItem] = {
    val selectAll = SELECT_SALARY_ITEM.where(company2 === companyId)
    execute(selectAll.to((EmployeeSalaryItem.apply _).tupled))
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
    salaryItems_ <- listEmployeeSalaryItem(companyId).runCollect.map(_.toList)
  } yield employees.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id),
                                     salaryItems = salaryItems_.filter(_.id == c.id)))

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
    salaryItems_ <- listEmployeeSalaryItem(company).runCollect.map(_.toList)
  }yield employees.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id),
                                    salaryItems = salaryItems_.filter(_.id == c.id)))
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
