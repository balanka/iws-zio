package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain._
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class UserRepositoryImpl(pool: ConnectionPool) extends UserRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  val users              =
    (int("id") ++ string("user_name") ++ string("first_name") ++ string("last_name") ++ string("hash") ++ string("phone")
      ++ string("email") ++ string("department") ++ string("menu")++ string("company") ++ int("modelid"))
      .table("users")
  val usersx =
    ( string("user_name") ++ string("first_name") ++ string("last_name") ++  string("hash")++ string("phone")
      ++ string("email") ++ string("department") ++ string("menu") ++ string("company") ++ int("modelid"))
      .table("users")

  val (id, userName, firstName, lastName, hash, phone, email, department, menu, company, modelid) = users.columns
  val (userName_, firstName_, lastName_,  hash_, phone_, email_, department_, menu_, company_, modelid_) = usersx.columns
  val X                = id ++ userName ++ firstName ++ lastName ++ hash ++ phone ++ email ++department ++ menu ++ company ++ modelid
  val XX               = userName_ ++ firstName_ ++ lastName_ ++ hash_ ++ phone_ ++ email_ ++ department_ ++ menu_ ++ company_ ++ modelid_
  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  override def create(c: User): ZIO[Any, RepositoryError, Unit]                         = {
    val query = insertInto(usersx)(XX).values(toTuple(c))

    ZIO.logDebug(s"Query to insert User is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create(models: List[User]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(u=>toTuple(u))
    val query = insertInto(usersx)(XX).values(data)
    ZIO.logDebug(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(users).where((id === item) && (company === companyId)))
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

  private def build(model: TYPE_)                                        =
    update(users)
      .set(firstName, model.firstName)
      .set(lastName, model.lastName)
      .set(email, model.email)
      .set(hash, model.hash)
      .set(phone, model.phone)
      .set(department, model.department)
      .set(menu, model.menu)
      .where((id === model.id) && (company === model.company))

  override def list(companyId: String): ZStream[Any, RepositoryError, User]                   = {
    val selectAll = select(X).from(users)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((User.apply _).tupled)).provideDriver(driverLayer)
  }
  override def getByUserName(name:String, companyId: String): ZIO[Any, RepositoryError, User]          = {
    val selectAll = select(X).from(users).where((userName === name) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((User.apply _).tupled))
        .findFirst(driverLayer, name)
  }

  override def getById(userId: Int, companyId: String): ZIO[Any, RepositoryError, User] = {
    val selectAll = select(X).from(users).where((id === userId) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((User.apply _).tupled))
        .findFirstInt(driverLayer, userId)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, User] = {
    val selectAll = select(X).from(users).where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((User.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }
  def toTuple(c:User)= (c.userName, c.firstName, c.lastName, c.hash, c.phone, c.email,   c.department, c.menu, c.company, c.modelid)

}

object UserRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, UserRepository] =
    ZLayer.fromFunction(new UserRepositoryImpl(_))
}
