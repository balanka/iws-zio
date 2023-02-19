package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{ User, User_ }
import zio._
import com.kabasoft.iws.repository.Schema.{ userSchema, userSchema_ }
import zio.sql.ConnectionPool
import zio.stream._

final class UserRepositoryImpl(pool: ConnectionPool) extends UserRepository with IWSTableDescriptionPostgres {

  val users  = defineTable[User]("users")
  val users_ = defineTable[User_]("users")

  val (id, userName, firstName, lastName, hash, phone, email, department, menu, company, modelid)       = users.columns
  val (userName_, firstName_, lastName_, hash_, phone_, email_, department_, menu_, company_, modelid_) = users_.columns

  lazy val driverLayer                                                            = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val SELECT                                                                      = select(id, userName, firstName, lastName, hash, phone, email, department, menu, company, modelid).from(users)
  override def create(c: User): ZIO[Any, RepositoryError, Unit]                   = {
    val query = insertInto(users_)(userName_, firstName_, lastName_, hash_, phone_, email_, department_, menu_, company_, modelid_).values(toTuple(c))

    ZIO.logDebug(s"Query to insert User is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create(models: List[User]): ZIO[Any, RepositoryError, Int]         = {
    val data  = models.map(u => (u.userName, u.firstName, u.lastName, u.hash, u.phone, u.email, u.department, u.menu, u.company, u.modelid))
    val query = insertInto(users_)(userName_, firstName_, lastName_, hash_, phone_, email_, department_, menu_, company_, modelid_).values(data)
    ZIO.logDebug(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(Id: Int, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(users).where((id === Id) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: User): ZIO[Any, RepositoryError, Int]        = {
    val update_ = build(model)
    ZIO.logDebug(s"Query Update vat is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }
  override def modify(models: List[User]): ZIO[Any, RepositoryError, Int] = {
    val update_ = models.map(build(_))
    // ZIO.logInfo(s"Query Update vat is ${renderUpdate(update_)}") *>
    executeBatchUpdate(update_)
      .provideLayer(driverLayer)
      .map(_.sum)
      .mapError(e => RepositoryError(e.getCause()))
  }

  private def build(model: User) =
    update(users)
      .set(firstName, model.firstName)
      .set(lastName, model.lastName)
      .set(email, model.email)
      .set(hash, model.hash)
      .set(phone, model.phone)
      .set(department, model.department)
      .set(menu, model.menu)
      .where((id === model.id) && (company === model.company))

  override def all(companyId: String): ZIO[Any, RepositoryError, List[User]] =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, User]                    = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((User.apply _).tupled)).provideDriver(driverLayer)
  }
  override def getByUserName(name: String, companyId: String): ZIO[Any, RepositoryError, User] = {
    val selectAll = SELECT.where((userName === name) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((User.apply _).tupled))
        .findFirst(driverLayer, name)
  }

  override def getById(userId: Int, companyId: String): ZIO[Any, RepositoryError, User]       = {
    val selectAll = SELECT.where((id === userId) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((User.apply _).tupled))
        .findFirstInt(driverLayer, userId)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, User] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((User.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }
  def toTuple(c: User)                                                                        = (c.userName, c.firstName, c.lastName, c.hash, c.phone, c.email, c.department, c.menu, c.company, c.modelid)

}

object UserRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, UserRepository] =
    ZLayer.fromFunction(new UserRepositoryImpl(_))
}
