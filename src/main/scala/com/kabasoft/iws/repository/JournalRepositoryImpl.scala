package com.kabasoft.iws.repository

import zio._
import com.kabasoft.iws.repository.Schema.journalSchema
import com.kabasoft.iws.repository.Schema.journal_Schema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{ Journal, Journal_ }
import zio.sql.ConnectionPool
import zio.stream._

final class JournalRepositoryImpl(pool: ConnectionPool) extends JournalRepository with IWSTableDescriptionPostgres {
  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val journals  = defineTable[Journal]("journal")
  val journals_ = defineTable[Journal_]("journal")
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
    account,
    oaccount,
    transdate,
    enterdate,
    postingdate,
    period,
    amount,
    idebit,
    debit,
    icredit,
    credit,
    currency,
    side,
    text,
    month,
    year,
    company,
    // file,
    modelid
  ) = journals.columns

  val (
    transidx,
    oidx,
    accountx,
    oaccountx,
    transdatex,
    enterdatex,
    postingdatex,
    periodx,
    amountx,
    idebitx,
    debitx,
    icreditx,
    creditx,
    currencyx,
    sidex,
    textx,
    monthx,
    yearx,
    companyx,
    // filex,
    modelidx
  ) = journals_.columns

  def tuple1(c: Journal)                                                           = (
    c.transid,
    c.oid,
    c.account,
    c.oaccount,
    c.transdate,
    c.enterdate,
    c.postingdate,
    c.period,
    c.amount,
    c.idebit,
    c.debit,
    c.icredit,
    c.credit,
    c.currency,
    c.side,
    c.text,
    c.month,
    c.year,
    c.company,
    // c.file,
    c.modelid
  )
  def tuple2(c: Journal_)                                                           = (
    c.transid,
    c.oid,
    c.account,
    c.oaccount,
    c.transdate,
    c.enterdate,
    c.postingdate,
    c.period,
    c.amount,
    c.idebit,
    c.debit,
    c.icredit,
    c.credit,
    c.currency,
    c.side,
    c.text,
    c.month,
    c.year,
    c.company,
    // c.file,
    c.modelid
  )
  override def create(c: Journal): ZIO[Any, RepositoryError, Unit]                 = {
    val query = insertInto(journals_)(
      transidx,
      oidx,
      accountx,
      oaccountx,
      transdatex,
      enterdatex,
      postingdatex,
      periodx,
      amountx,
      idebitx,
      debitx,
      icreditx,
      creditx,
      currencyx,
      sidex,
      textx,
      monthx,
      yearx,
      companyx /*, filex*/,
      modelidx
    )
      .values(tuple2(Journal_(c)))

    ZIO.logDebug(s"Query to insert Journal is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  val SELECT                                                                       = select(
    id,
    transid,
    oid,
    account,
    oaccount,
    transdate,
    enterdate,
    postingdate,
    period,
    amount,
    idebit,
    debit,
    icredit,
    credit,
    currency,
    side,
    text,
    month,
    year,
    company,
    modelid
  )
    .from(journals)
  override def create(models: List[Journal]): ZIO[Any, RepositoryError, Int]       = {
    val data  = models.map(Journal_.apply).map(tuple2)
    val query = insertInto(journals_)(
      transidx,
      oidx,
      accountx,
      oaccountx,
      transdatex,
      enterdatex,
      postingdatex,
      periodx,
      amountx,
      idebitx,
      debitx,
      icreditx,
      creditx,
      currencyx,
      sidex,
      textx,
      monthx,
      yearx,
      companyx,
      modelidx
    ).values(data)

    ZIO.logDebug(s"Query to insert Journal is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(Id: Long, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(journals).where(whereClause(Id, companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause))

  override def list(companyId: String): ZStream[Any, RepositoryError, Journal]        =
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(SELECT)}")
    ) *>
      execute(SELECT.to((Journal.apply _).tupled))
        .provideDriver(driverLayer)
  override def getBy(id: Long, companyId: String): ZIO[Any, RepositoryError, Journal] = {
    val selectAll = SELECT.where(whereClause(id, companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Journal.apply _).tupled))
        .findFirstLong(driverLayer, id)
  }

  override def getByModelId(modelid: Int, companyId: String): ZIO[Any, RepositoryError, Journal] = {
    val selectAll = SELECT.where(whereClause(modelid, companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Journal.apply _).tupled))
        .findFirstInt(driverLayer, modelid)
  }

  override def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, Journal] = {
    val selectAll = SELECT
      .where(whereClause(accountId, companyId, fromPeriod, toPeriod))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Journal.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object JournalRepositoryImpl {
  val live: ZLayer[ConnectionPool, RepositoryError, JournalRepository] =
    ZLayer.fromFunction(new JournalRepositoryImpl(_))
}
