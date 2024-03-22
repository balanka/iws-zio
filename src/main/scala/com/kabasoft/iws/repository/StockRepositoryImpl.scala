package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Stock
import com.kabasoft.iws.repository.Schema.stockSchema
import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.sql.Features.Literal
import zio.stream._
final class StockRepositoryImpl(pool: ConnectionPool) extends StockRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val stock = defineTable[Stock]("stock")
  val (id, store, article, quantity, charge, company, modelid) = stock.columns

  val SELECT  = select(id, store, article, quantity, charge, company, modelid).from(stock)


  def whereClause(Id: String,  companyId:String) =
     List((id === Id), (company === companyId))
      .fold(Expr.literal(true))(_ && _)
  def whereClause(storeId: String,  articleId: String, companyId:String) =
    List((article === articleId), (store === storeId), (company === companyId))
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

//  def whereClause(Ids: List[(String, String, String)]) =
//    Ids.flatMap(ids =>
//    List((store === ids._1), (article === ids._2), (company === ids._3))).fold(Expr.literal(true))(_ && _ )

  //def whereClause(Ids: List[(String, String)], companyId: String): List[Expr[Literal, Any, Boolean]] = Ids.map(id => whereClause(id._1,  id._2,  companyId))

  override def create(c: Stock): ZIO[Any, RepositoryError, Option[Stock]] = create2(c)*> getById(c.id)

  override def create(models: List[Stock]): ZIO[Any, RepositoryError, List[Stock]]              =
    if(models.isEmpty){
      ZIO.succeed(List.empty[Stock])
    }else {
      create2(models)*>(models.map(_.id).map(getById).flip).map(_.flatten)
    }

  override def create2(c: Stock): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(stock)(id, store, article, quantity, charge, company, modelid).values(Stock.unapply(c).get)
    ZIO.logInfo(s"Query to insert Stock is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create2(models: List[Stock]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Stock.unapply(_).get)
    val query = insertInto(stock)(id, store, article, quantity, charge, company, modelid ).values(data)

    ZIO.logInfo(s"Query to insert Store is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(Id: String, articleId:String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(stock).where(id === Id)
    ZIO.logInfo(s"Delete Store is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def modify(model: Stock): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(stock)
      .set(quantity, model.quantity)
      .where(id === model.id)
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
  override def getById(Id:String): ZIO[Any, RepositoryError, Option[Stock]]          = {
    val selectAll = SELECT.where(id === Id)
    ZIO.logInfo(s"Query to execute getById is ${renderRead(selectAll)}") *>
      execute(selectAll.to(st=> Stock.apply(st)))
        .provideDriver(driverLayer)
        .runCollect.map(_.toList).mapBoth( e=> RepositoryError(e.getMessage), l=>l.headOption).debug(s"getById>>> ${Id}")
  }
  override def getById(Ids:List[String]): ZIO[Any, RepositoryError, List[Stock]]          =  Ids.map(getById).flip.map(_.flatten)

  override def getBy(Id:(String, String)): ZIO[Any, RepositoryError, Stock]          = {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2))
    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(st=> Stock.apply(st)))
        .findFirst(driverLayer, Id._1)
  }

  override def getBy(Id:(String, String), company: String): ZIO[Any, RepositoryError, Option[Stock]]         =
  {
    val selectAll = SELECT.where(whereClause (Id._1, Id._2, company))
    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(st=> Stock.apply(st)))
        .provideDriver(driverLayer)
        .runCollect.map(_.toList).mapBoth( e=> RepositoryError(e.getMessage), l=>l.headOption).debug(s"getById>>> ${Id}")
  }
  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Stock]] =
    ids.map(id=>getBy((id, company))).flip


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
