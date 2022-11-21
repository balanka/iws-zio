package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain._
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class CostcenterRepositoryImpl(pool: ConnectionPool) extends CostcenterRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val module = (string("id") ++ string("name") ++ string("description") ++ string("account")
     ++ instant("enter_date")++ instant("modified_date")
    ++ instant("posting_date") ++ int("modelid") ++ string("company"))
    .table("costcenter")

  val (id, name, description, account, enterdate, changedate, postingdate, modelid, company)    = module.columns
  val X                                                                                = id ++ name ++ description ++ account ++ enterdate ++ changedate ++ postingdate ++ modelid ++ company
  val SELECT                                                                           = select(id, name, description, account, enterdate, changedate, postingdate, modelid, company).from(module)
  override def create(c: Costcenter): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(module)(X).values(Costcenter.unapply(c).get)

    ZIO.logInfo(s"Query to insert Costcenter is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Costcenter]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Costcenter.unapply(_).get)
    val query = insertInto(module)(X).values(data)

    ZIO.logInfo(s"Query to insert Costcenter is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(module).where((id === item.toLong) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Costcenter): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(module)
      .set(name, model.name)
      .set(description, model.description)
      .set(account, model.account)
      .where((id === model.id) && (company === model.company))
    ZIO.logInfo(s"Query Update Costcenter is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, Costcenter]                   = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Costcenter]          = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Costcenter] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

}

object CostcenterRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CostcenterRepository] =
    ZLayer.fromFunction(new CostcenterRepositoryImpl(_))
}
