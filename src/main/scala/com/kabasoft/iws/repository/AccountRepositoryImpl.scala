package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Account, Account_}
import com.kabasoft.iws.api.Protocol.account_schema
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class AccountRepositoryImpl(pool: ConnectionPool) extends AccountRepository with IWSTableDescriptionPostgres {


  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val map:List[Account] = List.empty[Account]


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

  val SELECT                                                                           = select(
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
  override def create(c: Account): ZIO[Any, RepositoryError, Unit]                     = {
    val query = insertInto(account)(id, name, description, enterdate, changedate, postingdate, company, modelid, accountid, isDebit, balancesheet, currency, idebit, icredit, debit, credit).values(tuple2(c))

    ZIO.logDebug(s"Query to insert Account is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Account]): ZIO[Any, RepositoryError, Int]           = {
    val data  = models.map(tuple2(_))
    val query = insertInto(account)(id, name, description, enterdate, changedate, postingdate, company, modelid, accountid, isDebit, balancesheet, currency, idebit, icredit, debit, credit).values(data)

    ZIO.logDebug(s"Query to insert Account is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(account).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  private def build(model: Account_) =
    update(account)
      .set(id, model.id)
      .set(name, model.name)
      .set(description, model.description)
      .set(company, model.company)
      .set(isDebit, model.isDebit)
      .set(balancesheet, model.balancesheet)
      .set(currency, model.currency)
      .where((id === model.id) && (company === model.company))

  override def modify(model: Account): ZIO[Any, RepositoryError, Int]        = {
    val update_ = build(Account_(model))
    ZIO.logDebug(s"Query Update Account is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }
  override def modify(models: List[Account]): ZIO[Any, RepositoryError, Int] = {
    val update_ = models.map(acc=>build( Account_(acc)))
   ZIO.foreach(update_.map(renderUpdate))(sql=>ZIO.logDebug(s"Query Update Account is ${sql}")) *>
    executeBatchUpdate(update_)
      .provideLayer(driverLayer)
      .map(_.sum)
      .mapError(e => RepositoryError(e.getCause()))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Account]] = for {
    accounts <- list(companyId).runCollect.map(_.toList)
  } yield accounts//.map(_.addSubAccounts(accounts))


  override def list(companyId: String): ZStream[Any, RepositoryError, Account] =
      ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(SELECT)}")) *>
      execute(SELECT.to(c => {val x = Account.apply(c); map :+ (x); x}))
        .provideDriver(driverLayer)

  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Account]          = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(c => (Account.apply(c))))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Account] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to(c => (Account.apply(c))))
        .findFirstInt(driverLayer, modelId)
  }

}

object AccountRepositoryImpl {
  val live: ZLayer[ConnectionPool, RepositoryError, AccountRepository] =
    ZLayer.fromFunction(new AccountRepositoryImpl(_))
}
