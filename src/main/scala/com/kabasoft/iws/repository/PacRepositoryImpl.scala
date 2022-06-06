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

  def getQuery(fromPeriod:Int, toPeriod:Int,  companyId:String) = {
    println(s" ${fromPeriod} ${toPeriod} ${companyId}  ")
    select((Max(id) as "id"), account, (Max(period) as "period"), (Min(idebit ) as "idebit"),  (Min(icredit) as "icredit")
      ,  (SumB(debit) as "debit"), (SumB(credit) as "credit" ), currency, company, modelid)
      .from(pac)
      .groupBy(account, currency, company, modelid)
      .where((company === companyId) && (period >=  fromPeriod) && (period <= toPeriod))
      .orderBy(account.descending)
  }

  val X = id ++ account ++ period ++ idebit ++ icredit ++ debit ++ credit ++ currency ++ company ++ modelid

  override def create(c: PeriodicAccountBalance): ZIO[Any, RepositoryError, Unit]           = {
    val query = insertInto(pac)(X)
      .values(PeriodicAccountBalance.unapply(c).get)

    ZIO.logInfo(s"Query to insert PeriodicAccountBalance is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int] = {
    val data = models.map(PeriodicAccountBalance.unapply(_).get)

    val query = insertInto(pac)(X).values(data)

    ZIO.logInfo(s"Query to insert PeriodicAccountBalance is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int]      =
    execute(deleteFrom(pac).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def list(companyId: String): ZStream[Any, RepositoryError, TYPE_]                   = {
    val selectAll = select(X)
      .from(pac)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, TYPE_]          = {
    val selectAll = select(X)
      .from(pac)
      .where((id === Id) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, TYPE_] = {
    val selectAll = select(X)
      .from(pac)
      .where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

  def findBalance4Period(fromPeriod: Int, toPeriod: Int, companyId: String):ZStream[Any, RepositoryError, TYPE_]  = {
    val query = getQuery(fromPeriod, toPeriod, companyId)
    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findBalance4Period is ${renderRead(query)}")
    ) *>
      execute(query.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }

  def find4Period(fromPeriod: Int, toPeriod: Int, companyId: String):ZStream[Any, RepositoryError, TYPE_] = {
    val selectAll = select(X)
      .from(pac)
      .where((company === companyId) && (period >=  fromPeriod) && (period <= toPeriod))
      .orderBy(account.descending)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }


}

object PacRepositoryImpl {

  val live: ZLayer[ConnectionPool, RepositoryError, PacRepository] =
    ZLayer.fromFunction(new PacRepositoryImpl(_))
}
