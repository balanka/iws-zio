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

  val (
    id,
    transid,
    // oid,
    account,
    oaccount,
    transdate,
    // enterdate,
    // postingdate,
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
    // oidx,
    accountx,
    oaccountx,
    transdatex,
    // enterdatex,
    // postingdatex,
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
    // c.oid,
    c.account,
    c.oaccount,
    c.transdate,
    // c.enterdate,
    // c.postingdate,
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
  def tuple2(c: Journal)                                                           = (
    c.id,
    c.transid,
    // c.oid,
    c.account,
    c.oaccount,
    c.transdate,
    // c.enterdate,
    // c.postingdate,
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
      transidx /*, oidx*/,
      accountx,
      oaccountx,
      transdatex /*, enterdatex, postingdatex*/,
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
      .values(tuple1(c))

    ZIO.logDebug(s"Query to insert Journal is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  val SELECT                                                                       = select(
    id,
    transid,
    account,
    oaccount,
    transdate,
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
    val data  = models.map(tuple1)
    val query = insertInto(journals_)(
      transidx,
      accountx,
      oaccountx,
      transdatex,
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
    execute(deleteFrom(journals).where((id === Id) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def list(companyId: String): ZStream[Any, RepositoryError, Journal]        =
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(SELECT)}")
    ) *>
      execute(SELECT.to((Journal.apply _).tupled))
        .provideDriver(driverLayer)
  override def getBy(Id: Long, companyId: String): ZIO[Any, RepositoryError, Journal] = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Journal.apply _).tupled))
        .findFirstLong(driverLayer, Id)
  }

  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Journal] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Journal.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

  override def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, Journal] = {
    val selectAll = SELECT
      .where((account === accountId) && (company === companyId) && (period >= fromPeriod) && (period <= toPeriod))
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
