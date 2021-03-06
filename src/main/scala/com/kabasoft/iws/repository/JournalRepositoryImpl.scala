package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain._
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class JournalRepositoryImpl(pool: ConnectionPool) extends JournalRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val journals         = (long("id") ++ long("transid") ++ long("oid") ++ string("account") ++ string("oaccount")
    ++ instant("transdate") ++ instant("enterdate") ++ instant("postingdate") ++ int("period") ++ bigDecimal("amount")
    ++ bigDecimal("idebit") ++ bigDecimal("debit") ++ bigDecimal("icredit") ++ bigDecimal("credit") ++ string(
      "currency"
    ) ++ boolean("side") ++ string("text") ++ int("month") ++ int("year")
    ++ string("company") ++ int("file_content") ++ int("modelid"))
    .table("journal")
  val journals1        = (long("transid") ++ long("oid") ++ string("account") ++ string("oaccount")
    ++ instant("transdate") ++ instant("enterdate") ++ instant("postingdate") ++ int("period") ++ bigDecimal("amount")
    ++ bigDecimal("idebit") ++ bigDecimal("debit") ++ bigDecimal("icredit") ++ bigDecimal("credit") ++ string(
      "currency"
    ) ++ boolean("side") ++ string("text") ++ int("month") ++ int("year")
    ++ string("company") ++ int("file_content") ++ int("modelid"))
    .table("journal")

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
    file,
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
    filex,
    modelidx
  ) = journals1.columns

  val X =
    id ++ transid ++ oid ++ account ++ oaccount ++ transdate ++ enterdate ++ postingdate ++ period ++ amount ++ idebit ++ debit ++ icredit ++ credit ++ currency ++ side ++ text ++ month ++ year ++ company ++ file ++ modelid

  val XX                                                                               =
    transidx ++ oidx ++ accountx ++ oaccountx ++ transdatex ++ enterdatex ++ postingdatex ++ periodx ++ amountx ++ idebitx ++ debitx ++ icreditx ++ creditx ++ currencyx ++ sidex ++ textx ++ monthx ++ yearx ++ companyx ++ filex ++ modelidx
  def tuple1(c: Journal)                                                               = (
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
    c.file,
    c.modelid
  )
  def tuple2(c: Journal)                                                               = (
    c.id,
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
    c.file,
    c.modelid
  )
  override def create(c: Journal): ZIO[Any, RepositoryError, Unit]                     = {
    val query = insertInto(journals1)(XX).values(tuple1(c))

    ZIO.logInfo(s"Query to insert Journal is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Journal]): ZIO[Any, RepositoryError, Int]           = {
    val data  = models.map(tuple1)
    val query = insertInto(journals1)(XX).values(data)

    ZIO.logInfo(s"Query to insert Journal is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(journals).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def list(companyId: String): ZStream[Any, RepositoryError, Journal]          = {
    val selectAll = select(X)
      .from(journals)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Journal.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Journal] = {
    val selectAll = select(X)
      .from(journals)
      .where((id === Id) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Journal.apply _).tupled))
        .findFirst(driverLayer, Id)
  }

  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Journal] = {
    val selectAll = select(X)
      .from(journals)
      .where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Journal.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

  def find4Period(fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, Journal] = {
    val selectAll = select(X)
      .from(journals)
      .where((company === companyId) && (period >= fromPeriod) && (period <= toPeriod))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Journal.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object JournalRepositoryImpl {
  val live: ZLayer[ConnectionPool, RepositoryError, JournalRepository] =
    ZLayer.fromFunction(new JournalRepositoryImpl(_))
}
