package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Asset
import com.kabasoft.iws.repository.Schema.assetSchema
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class AssetRepositoryImpl(pool: ConnectionPool) extends AssetRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val asset = defineTable[Asset]("asset")

  val (id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount, dep_method, amount, rate, life_span, scrap_value, frequency, currency) = asset.columns

  val SELECT= select(id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount, dep_method, amount, rate, life_span, scrap_value, frequency, currency).from(asset)


  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  override def create(c: Asset): ZIO[Any, RepositoryError, Asset]                        = create2(c)*>getBy((c.id, c.company))

  override def create(models: List[Asset]): ZIO[Any, RepositoryError, List[Asset]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Asset])
    }else {
      create2(models)*>getBy(models.map(_.id), models.head.company)
    }

  override def create2(c: Asset): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(asset)(id, name, description, changedate, enterdate, postingdate, company, modelid, account
      , oaccount, dep_method, amount, rate, life_span, scrap_value, frequency, currency).values(Asset.unapply(c).get)

    ZIO.logDebug(s"Query to insert asset is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Asset]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Asset.unapply(_).get)
    val query = insertInto(asset)(id, name, description, changedate, enterdate, postingdate, company, modelid, account
      , oaccount, dep_method, amount, rate, life_span,  scrap_value, frequency, currency).values(data)

    ZIO.logDebug(s"Query to insert asset is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(asset).where((company === companyId) && (id === item))
    ZIO.logDebug(s"Delete asset is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: Asset): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(asset)
      .set(name, model.name)
      .set(description, model.description)
      .set(account, model.account)
      .set(oaccount, model.oaccount)
      .set(scrap_value, model.scrapValue)
      .set(life_span, model.lifeSpan)
      .set(dep_method, model.depMethod)
      .set(amount, model.amount)
      .set(rate, model.rate)
      .set(frequency, model.frequency)
      .set(currency, model.currency)
      .where(whereClause( model.id,  model.company))
    ZIO.logDebug(s"Query Update asset is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Asset]] =
    list(Id).runCollect.map(_.toList)

  override def list(Id:(Int, String)): ZStream[Any, RepositoryError, Asset]                   = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Asset.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,String)): ZIO[Any, RepositoryError, Asset]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Asset.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Asset]] = for {
    banks <- getBy_(ids, company).runCollect.map(_.toList)
  } yield banks

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Asset] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Asset.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Asset]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Asset] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Asset.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object AssetRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, AssetRepository] =
    ZLayer.fromFunction(new AssetRepositoryImpl(_))
}
