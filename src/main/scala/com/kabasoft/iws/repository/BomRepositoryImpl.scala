package com.kabasoft.iws.repository
import com.kabasoft.iws.repository.Schema.bomSchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Bom

import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

final class BomRepositoryImpl(pool: ConnectionPool) extends BomRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val bom = defineTable[Bom]("bom")

  val (id, parent, quantity, description, company, modelid) = bom.columns

  val SELECT = select(id, parent, quantity, description, company, modelid).from(bom)


  def whereClause(artId: String, companyId: String) = {
    val list = if (artId.contains('*')) {
      List(company === companyId)
    } else {
      List(id === artId, company === companyId)
    }
    list.fold(Expr.literal(true))(_ && _)
  }

  override def create(models_ : List[Bom]): ZIO[Any, RepositoryError, Int] =
    if (models_.isEmpty) {
      ZIO.succeed(0)
    } else {
      val query = insertInto(bom)(id, parent, quantity, description, company, modelid).values(models_.map(toTuple))
      val insertSql = {
        renderInsert(query)
      }
      ZIO.logInfo(s"Query to insert Bom is ${insertSql}") *>
        execute(query).provideAndLog(driverLayer)
    }

  def modify(model: Bom): ZIO[Any, RepositoryError, Int] = {
    val update_ = build(model)
    execute(update_)
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))
  }

  private def build(model: Bom) =
    update(bom)
      .set(parent, model.parent)
      .set(description, model.description)
      .set(quantity, model.quantity)
      .set(company, model.company)
      .set(modelid, model.modelid)
      .where(whereClause(model.id, model.company))


  def modify(models: List[Bom]): ZIO[Any, RepositoryError, Int] =
    if (models.isEmpty) {
      ZIO.logInfo(s"Trying to update an empty List  update Bom is <<<<<<<<<<<<<}") *>
        ZIO.succeed(0)
    } else {
      val update_ = models.map(build)
      ZIO.logInfo(s"Trying to update an empty List  update Bom ${update_.map(renderUpdate)}") *>
        executeBatchUpdate(update_)
          .provideLayer(driverLayer)
          .mapBoth(e => RepositoryError(e.getMessage), _.sum)
    }

  override def list(companyId: String): ZStream[Any, RepositoryError, Bom] = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to((Bom.apply _).tupled))
      .provideDriver(driverLayer)
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Bom]] =
    list(companyId).runCollect.map(_.toList)

  override def getBy(id: String, companyId: String): ZIO[Any, RepositoryError, Bom] = {
    val selectAll = SELECT.where(whereClause(id, companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Bom.apply _).tupled))
        .findFirst(driverLayer, id, Bom.dummy)
  }

  override def getByIds(ids: List[String], companyId: String): ZIO[Any, RepositoryError, List[Bom]] =
    ids.map(id =>getBy(id, companyId)).flip

  def toTuple(bom: Bom) =
    (bom.id, bom.parent, bom.quantity, bom.company, bom.description, bom.modelid)
}

object BomRepositoryImpl {

  val live: ZLayer[ConnectionPool, RepositoryError, BomRepository] =
    ZLayer.fromFunction(new BomRepositoryImpl(_))
}
