package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Role, Role_, User, UserRight, UserRole, User_, Userx}
import zio._
import com.kabasoft.iws.repository.Schema.{role_Schema, userRightSchema, userRoleSchema, userSchema_, userSchemax}

import zio.sql.ConnectionPool
import zio.stream._

final class UserRepositoryImpl(pool: ConnectionPool) extends UserRepository with IWSTableDescriptionPostgres {

  val usersx  = defineTable[Userx]("users")
  val users = defineTable[User_]("users")
  val roles = defineTable[Role_]("role")
  val userRole = defineTable[UserRole]("user_role")
  val userRight = defineTable[UserRight]("user_right")

  val (id, userName, firstName, lastName, hash, phone, email, department, menu, company, modelid)       = usersx.columns
  val (userName_, firstName_, lastName_, hash_, phone_, email_, department_, menu_, company_, modelid_) = users.columns
  val (role_id,  role_name, role_description, transdate, postingdate, enterdate, role_modelid, role_company) = roles.columns
  val (userid, roleid,  company_r, modelid_r) = userRole.columns
  val (moduleid_rt,  roleid_rt, short, company_rt, modelid_rt) = userRight.columns

  lazy val driverLayer                                                            = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val SELECT                                                                      = select(id, userName, firstName, lastName, hash, phone, email, department, menu, company, modelid).from(usersx)
  val SELECT_ROLE = select(role_id,  role_name, role_description, transdate, postingdate, enterdate, role_modelid, role_company).from(roles)
  val SELECT_USER_ROLE = select(userid, roleid,  company_r, modelid_r).from(userRole)
  val SELECT_USER_RIGHT = select(moduleid_rt,  roleid_rt, short, company_rt, modelid_rt).from(userRight)
  def whereClause(Id: Int, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[Int], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: User): ZIO[Any, RepositoryError, User] = create2(c) *> getById(c.id, c.company)

  override def create(models: List[User]): ZIO[Any, RepositoryError, List[User]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[User])
    } else {
      create2(models) *> getBy(models.map(_.id), models.head.company)
    }
  override def create2(c: User): ZIO[Any, RepositoryError, Unit]                   = {
    val query = insertInto(users)(userName_, firstName_, lastName_, hash_, phone_, email_, department_, menu_, company_, modelid_).values(toTuple(c))

    ZIO.logDebug(s"Query to insert User is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create2(models: List[User]): ZIO[Any, RepositoryError, Int]         = {
    val data  = models.map(u => (u.userName, u.firstName, u.lastName, u.hash, u.phone, u.email, u.department, u.menu, u.company, u.modelid))
    val query = insertInto(users)(userName_, firstName_, lastName_, hash_, phone_, email_, department_, menu_, company_, modelid_).values(data)
    ZIO.logDebug(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(idx: Int, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(usersx).where((company === companyId) && (id === idx) ))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: User): ZIO[Any, RepositoryError, Int]        = {
    val update_ = build(model)
    ZIO.logDebug(s"Query Update vat is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }
  override def modify(models: List[User]): ZIO[Any, RepositoryError, Int] = {
    val update_ = models.map(build)
    // ZIO.logInfo(s"Query Update vat is ${renderUpdate(update_)}") *>
    executeBatchUpdate(update_)
      .provideLayer(driverLayer).mapBoth(e => RepositoryError(e.getMessage), _.sum)
  }

  private def build(model: User) =
    update(usersx)
      .set(userName, model.userName)
      .set(firstName, model.firstName)
      .set(lastName, model.lastName)
      .set(email, model.email)
      .set(hash, model.hash)
      .set(phone, model.phone)
      .set(department, model.department)
      .set(menu, model.menu)
      .where(whereClause(model.id, model.company))

  override def all(companyId: String): ZIO[Any, RepositoryError, List[User]] = for {
    users_ <- list(companyId).runCollect.map(_.toList)
    user_roles <- listUserRoles(companyId).runCollect.map(_.toList)
    _ <-ZIO.logDebug(s"listUserRoles is ${user_roles}")
    _ <-ZIO.foreachDiscard(users_)(u => ZIO.logDebug(s"user_role is ${ u.copy(rights = u.roles.flatMap(_.rights))}"))
    roles_ <- listRoles(companyId).runCollect.map(_.toList)
    _ <-ZIO.logDebug(s"Roles is ${roles_}")
    user_rights <- listUserRight(companyId).runCollect.map(_.toList)
    _ <-ZIO.logDebug(s"user_rights is ${user_rights}")
    _ <-ZIO.foreachDiscard(roles_)(r => ZIO.logDebug(s"user_role is ${r.copy(rights = user_rights.filter(rt => rt.roleid == r.id))}"))
  } yield {
    val  rolesx: List[Role] = roles_.map( r=>r.copy(rights = user_rights.filter(rt => rt.roleid == r.id)))
    val roles= rolesx.filter(r=> user_roles.filter(ur=>(ur.roleid == r.id)).map(ur=>ur.roleid).contains(r.id))
    val users  = users_.map(u => u.copy(roles = roles)).map(u=>  u.copy(rights = u.roles.flatMap(_.rights)))
      users
  }
  override def list(companyId: String): ZStream[Any, RepositoryError, User]                    = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to[User](c => User.apply(c))).provideDriver(driverLayer)
  }

  def listRoles(companyId: String): ZStream[Any, RepositoryError, Role] = {
    val selectAll = SELECT_ROLE.where((role_company === companyId))
    execute(selectAll.to[Role](c => Role.apply(c))).provideDriver(driverLayer)
  }
  def listUserRoles(companyId: String): ZStream[Any, RepositoryError, UserRole] = {
    val selectAll = SELECT_USER_ROLE.where(company_r === companyId)
    execute(selectAll.to((UserRole.apply _).tupled))
      .provideDriver(driverLayer)
  }

  def listUserRight(companyId: String): ZStream[Any, RepositoryError, UserRight] = {
    val selectAll = SELECT_USER_RIGHT.where(company_rt === companyId)
    execute(selectAll.to((UserRight.apply _).tupled)).provideDriver(driverLayer)
  }
  override def getByUserName(name: String, companyId: String): ZIO[Any, RepositoryError, User] = {
    val selectAll = SELECT.where((userName === name) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to[User](c => User.apply(c))).findFirst(driverLayer, name)
  }

  override def getById(userId: Int, companyId: String): ZIO[Any, RepositoryError, User]       = {
    val selectAll = SELECT.where((id === userId) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
    execute(selectAll.to[User](c => User.apply(c)))
        .findFirstInt(driverLayer, userId)
  }

  def getBy(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[User]] = for {
    cc <- getBy_(ids, company).runCollect.map(_.toList)
  } yield cc

  def getBy_(ids: List[Int], company: String): ZStream[Any, RepositoryError, User] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to[User](c => User.apply(c)))
      .provideDriver(driverLayer)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, User] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to[User](c => User.apply(c)))
        .findFirstInt(driverLayer, modelId)
  }
  def toTuple(c: User)                                                                        = ( c.userName, c.firstName, c.lastName, c.hash, c.phone, c.email, c.department, c.menu, c.company, c.modelid)

}

object UserRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, UserRepository] =
    ZLayer.fromFunction(new UserRepositoryImpl(_))
}
