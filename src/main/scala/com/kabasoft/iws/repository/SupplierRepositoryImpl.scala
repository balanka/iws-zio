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
      vatcode,
      company,
      modelid,
      enterdate,
      changedate,
      postingdate
    ).values(suppliers.map(toTuple))

  override def create2(models: List[Supplier]): ZIO[Any, RepositoryError, Int]        = buildInsert(models)
  override def create(model: Supplier): ZIO[Any, RepositoryError, Supplier] =
    create2(List(model)) *> getBy((model.id, model.company))

  override def create(models: List[Supplier]): ZIO[Any, RepositoryError, List[Supplier]]        =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Supplier])
    }  else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }
  @nowarn
   def buildInsert(models: List[Supplier]): ZIO[Any, RepositoryError, Int] = {
    val newBankAccounts = models.flatMap(_.bankaccounts.filter(_.modelid == -3).map(_.copy(modelid = 12)))
    val insertAll = buildInsertQuery(models)
    val insertBankAccounts = buildInsertBankAccount(newBankAccounts)
    val result = for {
      insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(insertBankAccounts.run)<*
        ZIO.logDebug(s" insert bank accounts stmt ${renderInsert(insertBankAccounts)}")
      inserted <- insertAll.run <* ZIO.logDebug(s"insert supplier  stmt ${renderInsert(insertAll)}")
    } yield insertedBankAccounts.getOrElse(0)  + inserted
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }

  override def create2(c: Supplier): ZIO[Any, RepositoryError, Int]                    = create2(List(c))

  private def buildDeleteBankAccount(ids: List[String]): List[Delete[BankAccount]] =
    ids.map(id=>deleteFrom(bankAccount).where(id_ === id))

  override def delete(idx: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(supplier).where((company === companyId) && (id === idx) )
    ZIO.logDebug(s"Delete supplier is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

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
    val oldBankAccounts = model.bankaccounts.filter(_.modelid == -2).map(_.copy(modelid = 12))
    val newBankAccounts = model.bankaccounts.filter(_.modelid == -3).map(_.copy(modelid = 12))
    val deleteBankAccounts = model.bankaccounts.filter(_.modelid == -1).map(_.id)
    val update_ = buildUpdate(Supplier_(model))
    val result = for {
       insertedBankAccounts <- ZIO.when(newBankAccounts.nonEmpty)(buildInsertBankAccount(newBankAccounts).run)
      updatedBankAccounts <- ZIO.when(oldBankAccounts.nonEmpty)(oldBankAccounts.map(ba => buildUpdateBankAccount(ba).run).flip.map(_.sum))
      deletedBankAccounts <- ZIO.when(deleteBankAccounts.nonEmpty)(buildDeleteBankAccount(deleteBankAccounts).map(_.run).flip.map(_.sum))
      updated <- update_.run
      _ <- ZIO.logInfo(s"New bank accounts insert stmt ${renderInsert(buildInsertBankAccount(newBankAccounts))}") *>
        ZIO.logInfo(s" bank accounts  update stmt ${oldBankAccounts.map(ba => renderUpdate(buildUpdateBankAccount(ba)))}") *>
        ZIO.logInfo(s" update supplier stm ${renderUpdate(update_)}") *>
        ZIO.logInfo(s"bank accounts to delete ${ buildDeleteBankAccount(deleteBankAccounts).map(renderDelete)}")
    } yield insertedBankAccounts.getOrElse(0) + updatedBankAccounts.getOrElse(0) + deletedBankAccounts.getOrElse(0) + updated
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }

  def listBankAccount(companyId: String): ZStream[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT_BANK_ACCOUNT.where(company_ === companyId)
    execute(selectAll.to((BankAccount.apply _).tupled))
      .provideDriver(driverLayer)
  }

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Supplier]] = for {
    suppliers     <- list(Id).runCollect.map(_.toList)
    bankAccounts_ <- listBankAccount(Id._2).runCollect.map(_.toList)
  } yield suppliers.map(c => c.copy(bankaccounts = bankAccounts_.filter(_.owner == c.id)))

  override def list(Id:(Int, String)): ZStream[Any, RepositoryError, Supplier]          = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)
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

    ZIO.logDebug(s"Query to execute getByIban is ${renderRead(selectAll)}") *>
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
