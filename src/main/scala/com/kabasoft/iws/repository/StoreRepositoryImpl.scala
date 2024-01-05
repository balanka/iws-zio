package com.kabasoft.iws.repository

import com.kabasoft.iws.repository.Schema.storeSchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Store
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class StoreRepositoryImpl(pool: ConnectionPool) extends StoreRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val store = defineTable[Store]("store")

  val (id, name, description, enterdate, changedate, postingdate, company, modelid) = store.columns

  val SELECT                                                                           = select(id, name, description, enterdate, changedate, postingdate, company, modelid).from(store)


  def whereClause(Id: String, companyId: String) =
    List((id === Id), (company === companyId))
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  override def create(c: Store): ZIO[Any, RepositoryError, Store]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[Store]): ZIO[Any, RepositoryError, List[Store]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Store])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: Store): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(store)(id, name, description, enterdate, changedate, postingdate, company, modelid).values(Store.unapply(c).get)

    ZIO.logDebug(s"Query to insert Store is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Store]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Store.unapply(_).get)
    val query = insertInto(store)(id, name, description, enterdate, changedate, postingdate, company, modelid ).values(data)

    ZIO.logDebug(s"Query to insert Store is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(idx: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(store).where((company === companyId) && (id === idx) )
    ZIO.logInfo(s"Delete Store is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: Store): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(store)
      .set(name, model.name)
      .set(description, model.description)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update Store is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Store]] =
    list(Id).runCollect.map(_.toList)

  override def list(Id:(Int,  String)): ZStream[Any, RepositoryError, Store]                   = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Store.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,String)): ZIO[Any, RepositoryError, Store]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Store.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Store]] = for {
    stores <- getBy_(ids, company).runCollect.map(_.toList)
  } yield stores

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Store] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Store.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Store]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Store] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Store.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object StoreRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, StoreRepository] =
    ZLayer.fromFunction(new StoreRepositoryImpl(_))
}
