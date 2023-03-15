package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Module
import com.kabasoft.iws.repository.Schema.moduleSchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class ModuleRepositoryImpl(pool: ConnectionPool) extends ModuleRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val module = defineTable[Module]("module")

  val (id, name, description, path, enterdate, changedate, postingdate, modelid, company) = module.columns
  val SELECT                                                                              = select(id, name, description, path, enterdate, changedate, postingdate, modelid, company).from(module)
  override def create(c: Module): ZIO[Any, RepositoryError, Unit]                         = {
    val query = insertInto(module)(id, name, description, path, enterdate, changedate, postingdate, modelid, company).values(Module.unapply(c).get)

    ZIO.logDebug(s"Query to insert Module is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Module]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(Module.unapply(_).get)
    val query = insertInto(module)(id, name, description, path, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Module is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int]    =
    execute(deleteFrom(module).where(company === companyId && id === item ))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: Module): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(module)
      .set(name, model.name)
      .set(description, model.description)
      .set(path, model.path)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query Update Module is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Module]]                  =
    list(companyId).runCollect.map(_.toList)
  override def list(companyId: String): ZStream[Any, RepositoryError, Module]                   = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Module.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,  String)): ZIO[Any, RepositoryError, Module]          = {
    val selectAll = SELECT.where((id === Id._1) && (company === Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Module.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Module]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Module] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Module.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object ModuleRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, ModuleRepository] =
    ZLayer.fromFunction(new ModuleRepositoryImpl(_))
}
