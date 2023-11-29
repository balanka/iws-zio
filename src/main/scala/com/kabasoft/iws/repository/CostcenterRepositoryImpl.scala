package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Costcenter
import com.kabasoft.iws.repository.Schema.costcenterSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class CostcenterRepositoryImpl(pool: ConnectionPool) extends CostcenterRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val module = defineTable[Costcenter]("costcenter")

  val (id, name, description, account, enterdate, changedate, postingdate, modelid, company) = module.columns
  val SELECT                                                                                 = select(id, name, description, account, enterdate, changedate, postingdate, modelid, company).from(module)

  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: Costcenter): ZIO[Any, RepositoryError, Costcenter] = create2(c) *> getBy((c.id, c.company))

  override def create(models: List[Costcenter]): ZIO[Any, RepositoryError, List[Costcenter]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[Costcenter])
    } else {
      create2(models) *> getBy(models.map(_.id), models.head.company)
    }
  override def create2(c: Costcenter): ZIO[Any, RepositoryError, Unit]                        = {
    val query =
      insertInto(module)(id, name, description, account, enterdate, changedate, postingdate, modelid, company).values(Costcenter.unapply(c).get)

    ZIO.logDebug(s"Query to insert Costcenter is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Costcenter]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Costcenter.unapply(_).get)
    val query = insertInto(module)(id, name, description, account, enterdate, changedate, postingdate, modelid, company).values(data)

    ZIO.logDebug(s"Query to insert Costcenter is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(idx: String, companyId: String): ZIO[Any, RepositoryError, Int]       =
    execute(deleteFrom(module).where((company === companyId) && (id === idx) ))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: Costcenter): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(module)
      .set(name, model.name)
      .set(description, model.description)
      .set(account, model.account)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query Update Costcenter is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Costcenter]]                  =
    list(companyId).runCollect.map(_.toList)
  override def list(companyId: String): ZStream[Any, RepositoryError, Costcenter]                   = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String, String)): ZIO[Any, RepositoryError, Costcenter]          = {
    val selectAll = SELECT.where((id === Id._1) && (company === Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Costcenter]] = for {
    cc <- getBy_(ids, company).runCollect.map(_.toList)
  } yield cc

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Costcenter] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Costcenter.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Costcenter]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Costcenter] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Costcenter.apply _).tupled))
        .provideDriver(driverLayer)
  }
}

object CostcenterRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, CostcenterRepository] =
    ZLayer.fromFunction(new CostcenterRepositoryImpl(_))
}
