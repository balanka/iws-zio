package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{TransactionLog, TransactionLog_}
import com.kabasoft.iws.repository.Schema.{transactionLogSchema, transactionLog_Schema}
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class TransactionLogRepositoryImpl(pool: ConnectionPool) extends TransactionLogRepository with IWSTableDescriptionPostgres {
  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val trans_log  = defineTable[TransactionLog]("transaction_log")
  val trans_log_ = defineTable[TransactionLog_]("transaction_log")
  def whereClause(modelId: Int, companyId: String) = List(modelid === modelId, company === companyId)
    .fold(Expr.literal(true))(_ && _)
  def whereClause(Id: Long, companyId: String) = List(id === Id, company === companyId)
    .fold(Expr.literal(true))(_ && _)
  def whereClause(accountId: String, companyId: String,from:Int, to:Int) =
    List(account === accountId, company === companyId, period >= from, period <= to)
      .fold(Expr.literal(true))(_ && _)

  val (
    id,
    transid,
    oid,
    store,
    account,
    article,
    quantity,
    stock,
    whole_stock,
    unit,
    price,
    avg_price,
    currency,
    duedate,
    text,
    transdate,
    postingdate,
    enterdate,
    period,
    company,
    modelid
  ) = trans_log.columns

  val (
    transidx,
    oidx,
    storex,
    accountx,
    articlex,
    quantityx,
    stockx,
    whole_stockx,
    unitx,
    pricex,
    avg_pricex,
    currencyx,
    duedatex,
    textx,
    transdatex,
    postingdatex,
    enterdatex,
    periodx,
    companyx,
    modelidx
  ) = trans_log_.columns

  def tuple2(c: TransactionLog_)                                                           = (
    c.transid,
    c.oid,
    c.store,
    c.costcenter,
    c.article,
    c.quantity,
    c.stock,
    c.wholeStock,
    c.unit,
    c.price,
    c.avgPrice,
    c.currency,
    c.duedate,
    c.text,
    c.transdate,
    c.postingdate,
    c.enterdate,
    c.period,
    c.company,
    c.modelid
  )
  override def create(c: TransactionLog): ZIO[Any, RepositoryError, Unit]                 = {
    val query = insertInto(trans_log_)(
      transidx,
      oidx,
      storex,
      accountx,
      articlex,
      quantityx,
      stockx,
      whole_stockx,
      unitx,
      pricex,
      avg_pricex,
      currencyx,
      duedatex,
      textx,
      transdatex,
      postingdatex,
      enterdatex,
      periodx,
      companyx,
      modelidx
    )
      .values(tuple2(TransactionLog_(c)))

    ZIO.logDebug(s"Query to insert TransactionLog is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  val SELECT = select(
    id,
    transid,
    oid,
    store,
    account,
    article,
    quantity,
    stock,
    whole_stock,
    unit,
    price,
    avg_price,
    currency,
    duedate,
    text,
    transdate,
    postingdate,
    enterdate,
    period,
    company,
    modelid
  )
    .from(trans_log)
  override def create(models: List[TransactionLog]): ZIO[Any, RepositoryError, Int]       =
    if(models.isEmpty) {
      ZIO.succeed(0)
    } else{
    val data  = models.map(TransactionLog_.apply).map(tuple2)
    val query = insertInto(trans_log_)(
      transidx,
      oidx,
      storex,
      accountx,
      articlex,
      quantityx,
      stockx,
      whole_stockx,
      unitx,
      pricex,
      avg_pricex,
      currencyx,
      duedatex,
      textx,
      transdatex,
      postingdatex,
      enterdatex,
      periodx,
      companyx,
      modelidx
    ).values(data)

    ZIO.logDebug(s"Query to insert TransactionLog is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(Id: Long, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(trans_log).where(whereClause(Id, companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def list(companyId: String): ZStream[Any, RepositoryError, TransactionLog]        =
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(SELECT)}")
    ) *>
      execute(SELECT.where(company === companyId)
        .to((TransactionLog.apply _).tupled))
        .provideDriver(driverLayer)
  override def getBy(id: Long, companyId: String): ZIO[Any, RepositoryError, TransactionLog] = {
    val selectAll = SELECT.where(whereClause(id, companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((TransactionLog.apply _).tupled))
        .findFirstLong(driverLayer, id)
  }

  override def getByModelId(modelid: Int, companyId: String): ZIO[Any, RepositoryError, TransactionLog] = {
    val selectAll = SELECT.where(whereClause(modelid, companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((TransactionLog.apply _).tupled))
        .findFirstInt(driverLayer, modelid)
  }

  override def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, TransactionLog] = {
    val selectAll = SELECT
      .where(whereClause(accountId, companyId, fromPeriod, toPeriod))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((TransactionLog.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object TransactionLogRepositoryImpl {
  val live: ZLayer[ConnectionPool, RepositoryError, TransactionLogRepository] =
    ZLayer.fromFunction(new TransactionLogRepositoryImpl(_))
}
