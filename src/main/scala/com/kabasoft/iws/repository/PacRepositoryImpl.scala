package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain._
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class PacRepositoryImpl(pool: ConnectionPool) extends PacRepository with IWSTableDescriptionPostgres {
  import ColumnSet._
  import AggregationDef._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val pac              =
    (string("id") ++ string("account") ++ int("period") ++ bigDecimal("idebit") ++ bigDecimal("icredit")
      ++ bigDecimal("debit") ++ bigDecimal("credit") ++ string("currency") ++ string("company") ++ int("modelid"))
      .table("periodic_account_balance")

  val (id, account, period, idebit, icredit, debit, credit, currency, company, modelid) = pac.columns
  val X                                                                                 = id ++ account ++ period ++ idebit ++ icredit ++ debit ++ credit ++ currency ++ company ++ modelid

  def getQuery(fromPeriod: Int, toPeriod: Int, companyId: String) =
    select(
      (Max(id) as "id"),
      account,
      (Max(period) as "period"),
      (SumB(idebit) as "idebit"),
      (SumB(icredit) as "icredit"),
      (SumB(debit) as "debit"),
      (SumB(credit) as "credit"),
      currency,
      company,
      modelid
    )
      .from(pac)
      .groupBy(account, currency, company, modelid)
      .where((company === companyId) && (period >= fromPeriod) && (period <= toPeriod))
      .orderBy(account.descending)

  def getBalancesQuery(fromPeriod: Int, toPeriod: Int, companyId: String) =
    select(
      (Max(id) as "id"),
      account,
      (Max(period) as "period"),
      (SumB(idebit) as "idebit"),
      (SumB(icredit) as "icredit"),
      (SumB(debit) as "debit"),
      (SumB(credit) as "credit"),
      currency,
      company,
      modelid
    )
      .from(pac)
      .groupBy(account, currency, company, modelid)
      .where((company === companyId) && (period >= fromPeriod) && (period <= toPeriod))
      .orderBy(account.descending)

   def createX(c: PeriodicAccountBalance): ZIO[Any, RepositoryError, Unit]             = {
    val query = insertInto(pac)(X)
      .values(PeriodicAccountBalance.unapply(c).get)

    ZIO.logDebug(s"Query to insert PeriodicAccountBalance is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models_ : List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int] =
    if (models_.isEmpty) {
      ZIO.succeed(0)
    } else {
      val data_ = models_.map(PeriodicAccountBalance.unapply(_).get)
      val query = insertInto(pac)(X).values(data_)

      ZIO.logDebug(s"Query to insert PeriodicAccountBalance is ${renderInsert(query)}") *>
        execute(query).provideAndLog(driverLayer)
    }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int]        =
    execute(deleteFrom(pac).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause))

  def modify(model: PeriodicAccountBalance): ZIO[Any, RepositoryError, Int] = {
    val update_ = build(model)
    execute(update_)
      .provideLayer(driverLayer)
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))
  }

  private def build(model: PeriodicAccountBalance) = {
   val query =
    update(pac)
      .set(idebit, model.idebit)
      .set(debit, model.debit)
      .set(icredit, model.icredit)
      .set(credit, model.credit)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query to update PeriodicAccountBalance is <<<<<<<<<<<<<${renderUpdate(query)}")
    query
  }

  def modify(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int] =
    if (models.isEmpty) {
      ZIO.logDebug(s"Trying to update an empty List  update PeriodicAccountBalance is <<<<<<<<<<<<<}")*>
      ZIO.succeed(0)
    } else {
      val update_ = models.map(build)
      executeBatchUpdate(update_)
        .provideLayer(driverLayer)
        .provideLayer(driverLayer)
        .mapBoth(e => RepositoryError(e.getCause), _.sum)
    }

  override def list(companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]          = {
    val selectAll = select(X).from(pac)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = select(X)
      .from(pac)
      .where((id === Id) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirst(driverLayer, Id, PeriodicAccountBalance.dummy)
  }

  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = select(X)
      .from(pac)
      .where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

  def findBalance4Period(
    fromPeriod: Int,
    toPeriod: Int,
    companyId: String
  ): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = getQuery(fromPeriod, toPeriod, companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findBalance4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to(x => PeriodicAccountBalance.applyX(x)))
        .provideDriver(driverLayer)
  }

  def getBalances4Period(fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val query = getBalancesQuery(fromPeriod, toPeriod, companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute getBalances4Period is ${renderRead(query)}")
    ) *>
      execute(query.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }

  def find4Period(fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = select(X)
      .from(pac)
      .where((company === companyId) && (period >= fromPeriod) && (period <= toPeriod))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
      .provideDriver(driverLayer)
  }
  def find4Period(accountId:String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = select(X)
      .from(pac)
      .where((account === accountId) &&(company === companyId) && (period >= fromPeriod) && (period <= toPeriod))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
      .provideDriver(driverLayer)
  }

}

object PacRepositoryImpl {

  val live: ZLayer[ConnectionPool, RepositoryError, PacRepository] =
    ZLayer.fromFunction(new PacRepositoryImpl(_))
}
