package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.repository.Schema.masterfileSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class MasterfileRepositoryImpl(pool: ConnectionPool) extends MasterfileRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val masterfile = defineTable[Masterfile]("masterfile")

  val (id, name, description, parent, enterdate, changedate, postingdate, modelid, company) = masterfile.columns
  val SELECT                                                                              = select(id, name, description, parent, enterdate, changedate, postingdate, modelid, company).from(masterfile)

  def whereClause(Id: String, modelId:Int, companyId: String) =
    List(id === Id, modelid === modelId, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], modelId:Int, companyId: String) =
    List(company === companyId, modelid === modelId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: Masterfile): ZIO[Any, RepositoryError, Masterfile] = create2(c) *> getBy((c.id, c.modelid, c.company))

  override def create(models: List[Masterfile]): ZIO[Any, RepositoryError, List[Masterfile]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[Masterfile])
    } else {
      create2(models) *> getBy(models.map(_.id), models.head.modelid, models.head.company)
    }
  override def create2(c: Masterfile): ZIO[Any, RepositoryError, Unit]                         = {
    val query = insertInto(masterfile)(id, name, description, parent, enterdate, changedate, postingdate, modelid, company).values(Masterfile.unapply(c).get)

    ZIO.logDebug(s"Query to insert Masterfile is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Masterfile]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(Masterfile.unapply(_).get)
    val query = insertInto(masterfile)(id, name, description, parent,enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Masterfile is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(idx: String, modelId:Int, companyId: String): ZIO[Any, RepositoryError, Int]    =
    execute(deleteFrom(masterfile).where((company === companyId) && (id === idx) && (modelid === modelId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: Masterfile): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(masterfile)
      .set(name, model.name)
      .set(description, model.description)
      .set(parent, model.parent)
      .where(whereClause(model.id,  model.modelid, model.company))
    ZIO.logDebug(s"Query Update Masterfile is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Masterfile]]                  =
    list(Id).runCollect.map(_.toList)

  override def list(Id:(Int, String)): ZStream[Any, RepositoryError, Masterfile]                   = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Masterfile.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,  Int, String)): ZIO[Any, RepositoryError, Masterfile]          = {
    val selectAll = SELECT.where(whereClause(Id._1, Id._2, Id._3))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Masterfile.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], modelId:Int, company: String): ZIO[Any, RepositoryError, List[Masterfile]] = for {
    vats <- getBy_(ids, modelId, company).runCollect.map(_.toList)
  } yield vats

  def getBy_(ids: List[String], modelId:Int, company: String): ZStream[Any, RepositoryError, Masterfile] = {
    val selectAll = SELECT.where(whereClause(ids, modelId, company))
    execute(selectAll.to((Masterfile.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Masterfile]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Masterfile] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Masterfile.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object MasterfileRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, MasterfileRepository] =
    ZLayer.fromFunction(new MasterfileRepositoryImpl(_))
}
