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

  val (id, name, description, path, parent, enterdate, changedate, postingdate, modelid, company) = module.columns
  val SELECT                                                                              = select(id, name, description, path, parent, enterdate, changedate, postingdate, modelid, company).from(module)

  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: Module): ZIO[Any, RepositoryError, Module] = create2(c) *> getBy((c.id, c.company))

  override def create(models: List[Module]): ZIO[Any, RepositoryError, List[Module]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[Module])
    } else {
      create2(models) *> getBy(models.map(_.id), models.head.company)
    }
  override def create2(c: Module): ZIO[Any, RepositoryError, Unit]                         = {
    val query = insertInto(module)(id, name, description, path, parent, enterdate, changedate, postingdate, modelid, company).values(Module.unapply(c).get)

    ZIO.logDebug(s"Query to insert Module is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Module]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(Module.unapply(_).get)
    val query = insertInto(module)(id, name, description, path, parent,enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Module is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int]    =
    execute(deleteFrom(module).where(whereClause(item, companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: Module): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(module)
      .set(name, model.name)
      .set(description, model.description)
      .set(path, model.path)
      .set(parent, model.parent)
      .where(whereClause(model.id,  model.company))
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
    val selectAll = SELECT.where(whereClause(Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Module.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Module]] = for {
    vats <- getBy_(ids, company).runCollect.map(_.toList)
  } yield vats

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Module] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Module.apply _).tupled))
      .provideDriver(driverLayer)
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
