package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Account, Account_}
import com.kabasoft.iws.repository.Schema.account_schema
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class AccountRepositoryImpl(pool: ConnectionPool) extends AccountRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer   = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val map: List[Account] = List.empty[Account]

  val account = defineTable[Account_]("account")

  def tuple2(acc: Account) = (
    acc.id,
    acc.name,
    acc.description,
    acc.enterdate,
    acc.changedate,
    acc.postingdate,
    acc.company,
    acc.modelid,
    acc.account,
    acc.isDebit,
    acc.balancesheet,
    acc.currency,
    acc.idebit,
    acc.icredit,
    acc.debit,
    acc.credit
  )

  val (
    id,
    name,
    description,
    enterdate,
    changedate,
    postingdate,
    company,
    modelid,
    accountid,
    isDebit,
    balancesheet,
    currency,
    idebit,
    icredit,
    debit,
    credit
  ) = account.columns

  val SELECT = select(
    id,
    name,
    description,
    enterdate,
    changedate,
    postingdate,
    company,
    modelid,
    accountid,
    isDebit,
    balancesheet,
    currency,
    idebit,
    icredit,
    debit,
    credit
  ).from(account)

  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  private def buildInsertQuery(accounts: List[Account]) =
    insertInto(account)(
      id,
      name,
      description,
      enterdate,
      changedate,
      postingdate,
      company,
      modelid,
      accountid,
      isDebit,
      balancesheet,
      currency,
      idebit,
      icredit,
      debit,
      credit
    ).values(accounts.map(tuple2))

  override def create(c: Account): ZIO[Any, RepositoryError, Account] =
    create2(c)*>getBy((c.id, c.company))
  override def create(c: List[Account]): ZIO[Any, RepositoryError, List[Account]] =
    if(c.isEmpty) {
      ZIO.succeed(List.empty[Account])
    }else {
      create2(c) *> getBy(c.map(_.id), c.head.company)
    }

  override def create2(models: List[Account]): ZIO[Any, RepositoryError, Int] = {
    val query = buildInsertQuery(models)
    ZIO.logDebug(s"Query to insert Account is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def create2(c: Account): ZIO[Any, RepositoryError, Unit]                     = {
    val query = buildInsertQuery(List(c))
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }

  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(account).where(whereClause(item, companyId))
    ZIO.logDebug(s"Delete Account is ${renderDelete(delete_)}") *>
    execute(delete_)
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))
  }

  private def build(model: Account_) =
    update(account)
      .set(id, model.id)
      .set(name, model.name)
      .set(description, model.description)
      .set(company, model.company)
      .set(isDebit, model.isDebit)
      .set(balancesheet, model.balancesheet)
      .set(currency, model.currency)
      .where(whereClause(model.id, model.company))

  override def modify(model: Account): ZIO[Any, RepositoryError, Int]        = {
    val update_ = build(Account_(model))
    ZIO.logDebug(s"Query Update Account is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }
  override def modify(models: List[Account]): ZIO[Any, RepositoryError, Int] = {
    val update_ = models.map(acc => build(Account_(acc)))
    ZIO.foreach(update_.map(renderUpdate))(sql => ZIO.logDebug(s"Query Update Account is ${sql}")) *>
      executeBatchUpdate(update_)
        .provideLayer(driverLayer)
        .map(_.sum)
        .mapError(e => RepositoryError(e.getMessage))
  }


  override def all(companyId: String): ZIO[Any, RepositoryError, List[Account]] = for {
    accounts <- list(companyId).runCollect.map(_.toList)
  } yield accounts // .map(_.addSubAccounts(accounts))

  override def list(companyId: String): ZStream[Any, RepositoryError, Account] =
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute findAll is ${renderRead(SELECT)}")) *>
      execute(SELECT.where(company === companyId)
        .to { c =>val x = Account.apply(c); map :+ (x); x
      })
        .provideDriver(driverLayer)

  override def getBy(Id: (String,  String)): ZIO[Any, RepositoryError, Account]          = {
    val selectAll = SELECT.where(whereClause(Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(c => (Account.apply(c))))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Account]] = for {
    accounts <- getBy_(ids, company).runCollect.map(_.toList)
  } yield accounts

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Account] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to[Account](c => Account.apply(c)))
      .provideDriver(driverLayer)
  }

  def getByModelId(id: (Int,  String)): ZIO[Any, RepositoryError, List[Account]]= for {
    accounts <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield accounts // .map(_.addSubAccounts(accounts))
  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Account] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to {c =>
          val x = Account.apply(c); map :+ (x); x
        }
        //selectAll.to(c => (Account.apply(c)))
        )
        .provideDriver(driverLayer)
  }

}

object AccountRepositoryImpl {
  val live: ZLayer[ConnectionPool, RepositoryError, AccountRepository] =
    ZLayer.fromFunction(new AccountRepositoryImpl(_))


}
