package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{UserRight, Role, Role_}
import com.kabasoft.iws.repository.Schema.{role_Schema, userRightSchema}
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class RoleRepositoryImpl(pool: ConnectionPool) extends RoleRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val userRole = defineTable[Role_]("user_role")
  val userRight = defineTable[UserRight]("user_right")


  val (id, name, description, transdate, postingdate, enterdate,  modelid, company) = userRole.columns
  val (moduleid, roleid, short, company_, modelid_)                                 = userRight.columns

  val SELECT                                                                         = select(id, name, description, transdate, postingdate, enterdate,  modelid, company).from(userRole)
  val SELECT_USER_RIGHT                                                              = select(moduleid, roleid, short, company_, modelid_).from(userRight)

  def toTuple(c: Role) = (c.id, c.name, c.description, c.transdate, c.postingdate, c.enterdate,  c.modelid, c.company)
  def whereClause(Id: Int, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[Int], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: Role): ZIO[Any, RepositoryError, Role]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[Role]): ZIO[Any, RepositoryError, List[Role]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Role])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: Role): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(userRole)(id, name, description, transdate, postingdate, enterdate,  modelid, company).values(toTuple(c))

    ZIO.logDebug(s"Query to insert user role is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Role]): ZIO[Any, RepositoryError, Int]              = {
    val query = insertInto(userRole)(id, name, description, transdate, postingdate, enterdate,  modelid, company).values(models.map(toTuple))

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

  override def modify(model: Role): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(userRole)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update user role is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Role]] = for {
    roles <- list(companyId).runCollect.map(_.toList)
    rights_ <- listUserRight(companyId).runCollect.map(_.toList)
  } yield roles.map(c => c.copy(rights = rights_.filter(_.roleid == c.id)))

  override def list(companyId: String): ZStream[Any, RepositoryError, Role]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to[Role](c => Role.apply(c)))
        .provideDriver(driverLayer)
  }

  def listUserRight(companyId: String): ZStream[Any, RepositoryError, UserRight] = {
    val selectAll = SELECT_USER_RIGHT.where(company_ === companyId)
    execute(selectAll.to((UserRight.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getBy(Id:(Int,String)): ZIO[Any, RepositoryError, Role]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Role](c => Role.apply(c)))
        .findFirstInt(driverLayer, Id._1)
  }

  def getBy(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Role]] = for {
    roles <- getBy_(ids, company).runCollect.map(_.toList)
  } yield roles

  def getBy_(ids: List[Int], company: String): ZStream[Any, RepositoryError, Role] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to[Role](c => Role.apply(c)))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Role]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Role] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[Role](c => Role.apply(c)))
        .provideDriver(driverLayer)
  }

}

object RoleRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, RoleRepository] =
    ZLayer.fromFunction(new RoleRepositoryImpl(_))
}
