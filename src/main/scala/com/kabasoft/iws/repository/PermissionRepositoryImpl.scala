package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Permission
import com.kabasoft.iws.repository.Schema.permissionSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class PermissionRepositoryImpl(pool: ConnectionPool) extends PermissionRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  private val permission = defineTable[Permission]("permission")

  val (id, name, description, enterdate, changedate, postingdate, modelid, company) = permission.columns

  val SELECT                                                                           = select(id, name, description, enterdate, changedate, postingdate, modelid, company).from(permission)


  def whereClause(Id: Int, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[Int], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  override def create(c: Permission): ZIO[Any, RepositoryError, Permission]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[Permission]): ZIO[Any, RepositoryError, List[Permission]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Permission])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: Permission): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(permission)(id, name, description, enterdate, changedate, postingdate, modelid, company).values(Permission.unapply(c).get)

    ZIO.logDebug(s"Query to insert Permission is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Permission]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Permission.unapply(_).get)
    val query = insertInto(permission)(id, name, description, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Permission is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(id: Int, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(permission).where(whereClause (id, companyId))
    ZIO.logInfo(s"Delete Permission is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: Permission): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(permission)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update Permission is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Permission]] = {
    val l = list(companyId).runCollect.map(_.toList)
      l
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, Permission]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Permission.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(Int,String)): ZIO[Any, RepositoryError, Permission]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Permission.apply _).tupled))
        .findFirstInt(driverLayer, Id._1)
  }

  def getBy(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Permission]] = for {
    permissions <- getBy_(ids, company).runCollect.map(_.toList)
  } yield permissions

  def getBy_(ids: List[Int], company: String): ZStream[Any, RepositoryError, Permission] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Permission.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Permission]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Permission] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Permission.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object PermissionRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, PermissionRepository] =
    ZLayer.fromFunction(new PermissionRepositoryImpl(_))
}
