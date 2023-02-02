package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Costcenter
import com.kabasoft.iws.api.Protocol.costCenterSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class CostcenterRepositoryImpl(pool: ConnectionPool) extends CostcenterRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val module = defineTable[Costcenter]("costcenter")


  val (id, name, description, account, enterdate, changedate, postingdate, modelid, company)    = module.columns
  val SELECT                                                                           = select(id, name, description, account, enterdate, changedate, postingdate, modelid, company).from(module)
  override def create(c: Costcenter): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(module)(id, name, description, account, enterdate, changedate, postingdate, modelid, company).values(Costcenter.unapply(c).get)

    ZIO.logDebug(s"Query to insert Costcenter is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Costcenter]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Costcenter.unapply(_).get)
    val query = insertInto(module)(id, name, description, account, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Costcenter is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(module).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Costcenter): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(module)
      .set(name, model.name)
      .set(description, model.description)
      .set(account, model.account)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query Update Costcenter is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, Costcenter]                   = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Costcenter]          = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Costcenter] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

}

object CostcenterRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CostcenterRepository] =
    ZLayer.fromFunction(new CostcenterRepositoryImpl(_))
}
