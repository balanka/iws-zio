package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.UserRole
import com.kabasoft.iws.repository.Schema.roleSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class RoleRepositoryImpl(pool: ConnectionPool) extends RoleRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val userRole = defineTable[UserRole]("userrole")

  val (id, name, description, enterdate, changedate, postingdate, modelid, company) = userRole.columns

  val SELECT                                                                           = select(id, name, description, enterdate, changedate, postingdate, modelid, company).from(userRole)


  def whereClause(Id: Int, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[Int], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: UserRole): ZIO[Any, RepositoryError, UserRole]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[UserRole]): ZIO[Any, RepositoryError, List[UserRole]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[UserRole])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: UserRole): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(userRole)(id, name, description, enterdate, changedate, postingdate, modelid, company).values(UserRole.unapply(c).get)

    ZIO.logDebug(s"Query to insert user role is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[UserRole]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(UserRole.unapply(_).get)
    val query = insertInto(userRole)(id, name, description, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert user role is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(id: Int, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(userRole).where(whereClause (id, companyId))
    ZIO.logInfo(s"Delete user role is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: UserRole): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(userRole)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update user role is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[UserRole]] =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, UserRole]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((UserRole.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(Int,String)): ZIO[Any, RepositoryError, UserRole]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((UserRole.apply _).tupled))
        .findFirstInt(driverLayer, Id._1)
  }

  def getBy(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[UserRole]] = for {
    roles <- getBy_(ids, company).runCollect.map(_.toList)
  } yield roles

  def getBy_(ids: List[Int], company: String): ZStream[Any, RepositoryError, UserRole] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((UserRole.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[UserRole]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, UserRole] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((UserRole.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object RoleRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, RoleRepository] =
    ZLayer.fromFunction(new RoleRepositoryImpl(_))
}
