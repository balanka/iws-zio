package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain._
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class PacRepositoryImpl(pool: ConnectionPool) extends PacRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val pac              =
    (string("id") ++ string("account") ++ int("period") ++ bigDecimal("idebit") ++ bigDecimal("icredit") ++ bigDecimal(
      "debit"
    )
      ++ bigDecimal("credit") ++ string("currency") ++ string("company") ++ int("modelid"))
      .table("periodic_account_balance")

  val (id, account, period, idebit, icredit, debit, credit, currency, company, modelid) = pac.columns

  val X = id ++ account ++ period ++ idebit ++ icredit ++ debit ++ credit ++ currency ++ company ++ modelid

  override def create(c: PeriodicAccountBalance): ZIO[Any, RepositoryError, Unit]           = {
    val query = insertInto(pac)(X)
      .values(PeriodicAccountBalance.unapply(c).get)

    ZIO.logInfo(s"Query to insert PeriodicAccountBalance is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int] = {
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

  override def list(companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]                   = {
    val selectAll = select(X)
      .from(pac)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, PeriodicAccountBalance]          = {
    val selectAll = select(X)
      .from(pac)
      .where((id === Id) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, PeriodicAccountBalance] = {
    val selectAll = select(X)
      .from(pac)
      .where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((PeriodicAccountBalance.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

}

object PacRepositoryImpl {

  val live: ZLayer[ConnectionPool, Throwable, PacRepository] =
    ZLayer.fromFunction(new PacRepositoryImpl(_))
}
