package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.ImportFile
import com.kabasoft.iws.repository.Schema.importFileSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class ImportFileRepositoryImpl(pool: ConnectionPool) extends ImportFileRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val importFile = defineTable[ImportFile]("bankstatement_file")

  val (id, name, description, extension, enterdate, changedate, postingdate, modelid, company) = importFile.columns

  val SELECT                                                                           = select(id, name, description, extension, enterdate, changedate, postingdate, modelid, company).from(importFile)


  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  override def create(c: ImportFile): ZIO[Any, RepositoryError, ImportFile]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[ImportFile]): ZIO[Any, RepositoryError, List[ImportFile]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[ImportFile])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: ImportFile): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(importFile)(id, name, description,  extension, enterdate, changedate, postingdate, modelid, company).values(ImportFile.unapply(c).get)

    ZIO.logDebug(s"Query to insert ImportFile is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[ImportFile]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(ImportFile.unapply(_).get)
    val query = insertInto(importFile)(id, name, description,  extension, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert ImportFile is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(idx: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(importFile).where((company === companyId) && (id === idx)  )
    ZIO.logInfo(s"Delete ImportFile is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: ImportFile): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(importFile)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update ImportFile is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[ImportFile]] =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, ImportFile]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((ImportFile.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,String)): ZIO[Any, RepositoryError, ImportFile]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((ImportFile.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[ImportFile]] = for {
    banks <- getBy_(ids, company).runCollect.map(_.toList)
  } yield banks

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, ImportFile] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((ImportFile.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[ImportFile]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, ImportFile] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((ImportFile.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object ImportFileRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, ImportFileRepository] =
    ZLayer.fromFunction(new ImportFileRepositoryImpl(_))
}
