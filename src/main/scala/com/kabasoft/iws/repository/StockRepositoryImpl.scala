package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Stock
import com.kabasoft.iws.repository.Schema.stockSchema
import zio._
import zio.sql.ConnectionPool
import zio.sql.Features.Literal
import zio.stream._
final class StockRepositoryImpl(pool: ConnectionPool) extends StockRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val stock = defineTable[Stock]("stock")
  val (store, article, quantity, charge, company, modelid) = stock.columns

  val SELECT  = select(store, article, quantity, charge, company, modelid).from(stock)


  def whereClause(storeId: String, articleId: String, companyId:String) =
     List((store === storeId), (article === articleId), (company === companyId))
      .fold(Expr.literal(true))(_ && _)



  def whereClause(Ids: List[(String, String, String)]) =
    Ids.flatMap(ids =>
    List((store === ids._1), (article === ids._2), (company === ids._3))).fold(Expr.literal(true))(_ && _ )

  //def whereClause(Ids: List[(String, String)], companyId: String): List[Expr[Literal, Any, Boolean]] = Ids.map(id => whereClause(id._1,  id._2,  companyId))

  override def create(c: Stock): ZIO[Any, RepositoryError, Stock] = create2(c)*>
    getBy((c.store, c.article,c.company))

  override def create(models: List[Stock]): ZIO[Any, RepositoryError, List[Stock]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Stock])
    }else {
      create2(models)*>getBy(models.map(m=>(m.store, m.article)), models.head.company)
    }

  override def create2(c: Stock): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(stock)(store, article, quantity, charge, company, modelid).values(Stock.unapply(c).get)

    ZIO.logDebug(s"Query to insert Store is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Stock]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Stock.unapply(_).get)
    val query = insertInto(stock)(store, article, quantity, charge, company, modelid ).values(data)

    ZIO.logDebug(s"Query to insert Store is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(storeId: String, articleId:String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(stock).where((article === articleId) && (store === storeId) && (company === companyId) )
    ZIO.logInfo(s"Delete Store is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: Stock): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(stock)
      .set(quantity, model.quantity)
      .where(whereClause( model.store,  model.article, model.company))
    ZIO.logDebug(s"Query Update Store is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Stock]] =
    list(Id).runCollect.map(_.toList)

  override def list(Id:(Int,  String)): ZStream[Any, RepositoryError, Stock]                   = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to(st=> Stock.apply(st)))
        .provideDriver(driverLayer)
  }
  override def getBy(Id:(String,String, String)): ZIO[Any, RepositoryError, Stock]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2, Id._3))
    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(st=> Stock.apply(st)))
        .findFirst(driverLayer, Id._1)
  }

  override def getBy(Id:(String,String)): ZIO[Any, RepositoryError, List[Stock]]          = {
    val selectAll = SELECT.where((article === Id._1) || (store === Id._1) && (company === Id._2))

    ZIO.logDebug(s"Query to execute findBy article or store is ${renderRead(selectAll)}") *>
      execute(selectAll.to(st=> Stock.apply(st)))
        .provideDriver(driverLayer)
        .runCollect.map(_.toList)
  }

  def getBy(ids: List[(String, String)], company: String): ZIO[Any, RepositoryError, List[Stock]] = for {
    stores <- getBy_(ids, company).runCollect.map(_.toList)
  } yield stores

  def getBy_(ids: List[(String, String)], company: String): ZStream[Any, RepositoryError, Stock] = {
    val selectAll = SELECT.where(whereClause(ids.map(ids =>(ids._1, ids._2,  company))))
    execute(selectAll.to(st=> Stock.apply(st)))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Stock]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Stock] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(st=> Stock.apply(st)))
        .provideDriver(driverLayer)
  }

}

object StockRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, StockRepository] =
    ZLayer.fromFunction(new StockRepositoryImpl(_))
}
