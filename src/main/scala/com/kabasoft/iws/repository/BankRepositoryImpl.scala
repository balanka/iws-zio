package com.kabasoft.iws.repository

import zio._
import zio.stream._
import zio.sql.ConnectionPool
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError._

final class BankRepositoryImpl(pool: ConnectionPool) extends BankRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val bank = (string("id") ++ string("name") ++ string("description") ++ instant("enter_date")
    ++ instant("modified_date") ++ instant("posting_date") ++ int("modelid") ++ string("company"))
    .table("bank")

  val (id, name, description, enterdate, changedate, postingdate, modelid, company)    = bank.columns
  val X                                                                                = id ++ name ++ description ++ enterdate ++ changedate ++ postingdate ++ modelid ++ company
  val SELECT                                                                           = select(id, name, description, enterdate, changedate, postingdate, modelid, company).from(bank)
  override def create(c: Bank): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(bank)(X).values(Bank.unapply(c).get)

    ZIO.logInfo(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Bank]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Bank.unapply(_).get)
    val query = insertInto(bank)(X).values(data)

    ZIO.logInfo(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(bank).where((id === item.toLong) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Bank): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bank)
      .set(name, model.name)
      .set(description, model.description)
      .where((id === model.id) && (company === model.company))
    ZIO.logInfo(s"Query Update Bank is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Bank]]                  =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, Bank]                   = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Bank.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Bank]          = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Bank.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Bank] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Bank.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

}

object BankRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, BankRepository] =
    ZLayer.fromFunction(new BankRepositoryImpl(_))
}
