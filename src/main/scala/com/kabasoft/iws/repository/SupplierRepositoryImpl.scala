package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{BankAccount, Supplier, Supplier_}
import zio._
import com.kabasoft.iws.repository.Schema.{bankAccountSchema, supplier_Schema}
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

import scala.annotation.nowarn

final class SupplierRepositoryImpl(pool: ConnectionPool) extends SupplierRepository with IWSTableDescriptionPostgres {
  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val supplier                                = defineTable[Supplier_]("supplier")
  val bankAccount                             = defineTable[BankAccount]("bankaccount")
  val (id_, bic, owner, company_, modelid_) = bankAccount.columns

  def whereClause(Idx: String, companyId: String) =
    List(id === Idx, company === companyId).fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

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
    iban,
    vatcode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
  ) = supplier.columns

  val SELECT               = select(
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
  )
    .from(supplier)
  val SELECT2              = select(
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
    id_,
    vatcode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
  )
    .from(supplier.join(bankAccount).on(id === owner))
  def toTuple(c: Supplier) = (
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

  private def buildInsertBankAccount(ba: List[BankAccount]) =
    insertInto(bankAccount)(id_, bic, owner, company_, modelid_).values(ba.map(BankAccount.unapply(_).get))

  private def buildUpdateBankAccount(model: BankAccount): Update[BankAccount] =
    update(bankAccount)
      .set(bic, model.bic)
      .set(owner, model.owner)
      .set(company_, model.company)
      .where(id_ === model.id)
  private def buildInsertQuery(suppliers: List[Supplier]) =
    insertInto(supplier)(
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
    ).values(suppliers.map(toTuple))
  override def create2(c: Supplier): ZIO[Any, RepositoryError, Unit]                    = {
    val query = buildInsertQuery(List(c))
    ZIO.logDebug(s"Query to insert Supplier is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }

  private def buildDeleteBankAccount(ids: List[String]): Delete[BankAccount] =
    deleteFrom(bankAccount).where(id_ in ids)

  override def create2(models: List[Supplier]): ZIO[Any, RepositoryError, Int] = {
    val query = buildInsertQuery(models)
    ZIO.logInfo(s"Query to insert Supplier is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }

  override def create(c: Supplier): ZIO[Any, RepositoryError, Supplier] =
    create2(c) *> getBy((c.id, c.company))
  override def create(models: List[Supplier]): ZIO[Any, RepositoryError, List[Supplier]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[Supplier])
    } else {
      create2(models) *> getBy(models.map(_.id), models.head.company)
    }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(supplier).where(whereClause(item, companyId))
    ZIO.logDebug(s"Delete supplier is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

   def  splitBankAccounts(s:Supplier, persistentBankAccounts: List[BankAccount], flag:Boolean): List[BankAccount] =
     for {
       bankAccounts <- if(flag) s.bankaccounts.filter(ba => persistentBankAccounts.map(_.id).contains(ba.id))
                       else s.bankaccounts.filterNot(ba => persistentBankAccounts.map(_.id).contains(ba.id))
     } yield bankAccounts

  private def buildUpdate(model: Supplier_):Update[Supplier_]  =
    update(supplier)
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

  override def update(model: Supplier): ZIO[Any, RepositoryError, Supplier] =
    modify(model)*>getBy((model.id, model.company))
  @nowarn
  override def modify(model: Supplier): ZIO[Any, RepositoryError, Int] = {
    val result = for {
      persistentBankAccounts <- getBankAccounts4Supplier(model.id, model.company)
      oldBankAccounts = splitBankAccounts(model, persistentBankAccounts, true)
      newBankAccounts = splitBankAccounts(model, persistentBankAccounts, false)
      deleteBankAccounts = model.bankaccounts.filter(_.modelid == -1)
      update_ = buildUpdate(Supplier_(model))
      insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(buildInsertBankAccount(newBankAccounts).run)
      updatedBankAccounts <- ZIO.when(oldBankAccounts.nonEmpty)(oldBankAccounts.map(ba => buildUpdateBankAccount(ba).run).flip.map(_.sum))
      deletedBankAccounts <- ZIO.when(deleteBankAccounts.nonEmpty)(buildDeleteBankAccount(deleteBankAccounts.map(_.id)).run)
      updated <- update_.run
      _ <- ZIO.logInfo(s"New bank accounts insert stmt ${renderInsert(buildInsertBankAccount(newBankAccounts))}") *>
        ZIO.logInfo(s"bank accounts to update ${renderUpdate(update_)}") *>
        ZIO.logInfo(s"bank accounts to delete ${renderDelete(buildDeleteBankAccount(deleteBankAccounts.map(_.id)))}")
    } yield insertedBankAccounts.getOrElse(0) + updatedBankAccounts.getOrElse(0) + deletedBankAccounts.getOrElse(0) + updated
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }

  def listBankAccount(companyId: String): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT.where(company_ === companyId)
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }

  def getBankAccounts4Supplier(Id:String, companyId: String): ZIO[Any, RepositoryError, List[BankAccount]] = {
    val selectAll = SELECT_BANK_ACCOUNT.where( (owner === Id) && (company_ === companyId))
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer).runCollect.map(_.toList)
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Supplier]] = for {
    suppliers     <- list(companyId).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(companyId).runCollect.map(_.toList)
  } yield suppliers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def list(companyId: String): ZStream[Any, RepositoryError, Supplier]          = {
    val selectAll = SELECT
    execute(selectAll.to[Supplier](c => Supplier.apply(c)))
      .provideDriver(driverLayer)
  }


  override def getBy(id:(String,String)): ZIO[Any, RepositoryError, Supplier] = {
    val selectAll = SELECT.where(whereClause(id._1, id._2))
    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirst(driverLayer, id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Supplier]] = for {
    customers <- getBy_(ids, company).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(company).runCollect.map(_.toList)
  } yield customers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Supplier] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to[Supplier](c => Supplier.apply(c)))
      .provideDriver(driverLayer)
  }
  override def getByIban(iban: String, companyId: String): ZIO[Any, RepositoryError, Supplier]    = {
    val selectAll = SELECT2.where((id_ === iban) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByIban is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .findFirst(driverLayer, iban)
  }

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Supplier]] = for {
    suppliers <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(id._2).runCollect.map(_.toList)
  } yield suppliers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Supplier] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[Supplier](c => Supplier.apply(c)))
        .provideDriver(driverLayer)
  }
}
object SupplierRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, SupplierRepository] =
    ZLayer.fromFunction(new SupplierRepositoryImpl(_))
}
