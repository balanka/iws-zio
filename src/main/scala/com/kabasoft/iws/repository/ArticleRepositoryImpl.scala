package com.kabasoft.iws.repository
import com.kabasoft.iws.repository.Schema.article_Schema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Article, Article_}
import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

final class ArticleRepositoryImpl(pool: ConnectionPool) extends ArticleRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer   = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val map: List[Article] = List.empty[Article]

  val articles = defineTable[Article_]("article")

  def tuple2(art: Article) = (
    art.id,
    art.name,
    art.description,
    art.parent,
    art.sprice,
    art.pprice,
    art.avgPrice,
    art.currency,
    art.stocked,
    art.quantityUnit,
    art.packUnit,
    art.stockAccount,
    art.expenseAccount,
    art.vatCode,
    art.company,
    art.modelid,
    art.enterdate,
    art.changedate,
    art.postingdate
    //art.bom
  )

  val (
    id,
    name,
    description,
    parent,
    sprice,
    pprice,
    avgPrice,
    currency,
    stocked,
    quantityUnit,
    packUnit,
    stockAccount,
    expenseAccount,
    vatCode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
    ) = articles.columns

  val SELECT = select(
    id,
    name,
    description,
    parent,
    sprice,
    pprice,
    avgPrice,
    currency,
    stocked,
    quantityUnit,
    packUnit,
    stockAccount,
    expenseAccount,
    vatCode,
    company,
    modelid,
    enterdate,
    changedate,
    postingdate
  ).from(articles)

  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)
  private def buildInsertQuery(models: List[Article]) =
    insertInto(articles)(
      id,
      name,
      description,
      parent,
      sprice,
      pprice,
      avgPrice,
      currency,
      stocked,
      quantityUnit,
      packUnit,
      stockAccount,
      expenseAccount,
      vatCode,
      company,
      modelid,
      enterdate,
      changedate,
      postingdate
    ).values(models.map(tuple2))

  override def create(c: Article): ZIO[Any, RepositoryError, Article] =
    create2(c)*>getBy((c.id, c.company))
  override def create(c: List[Article]): ZIO[Any, RepositoryError, List[Article]] =
    if(c.isEmpty) {
      ZIO.succeed(List.empty[Article])
    }else {
      create2(c) *> getBy(c.map(_.id), c.head.company)
    }

  override def create2(models: List[Article]): ZIO[Any, RepositoryError, Int] = {
    val query = buildInsertQuery(models)
    ZIO.logDebug(s"Query to insert Article is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def create2(c: Article): ZIO[Any, RepositoryError, Unit]                     = {
    val query = buildInsertQuery(List(c))
    execute(query)
      .provideAndLog(driverLayer)
      .unit
  }

  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(articles).where((company === companyId) && (id === item))
    ZIO.logDebug(s"Delete Article is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }


  private def build(model: Article_) =
    update(articles)
      .set(id, model.id)
      .set(name, model.name)
      .set(description, model.name)
      .set(parent, model.parent)
      .set(sprice, model.sprice)
      .set(pprice, model.pprice)
      .set(avgPrice, model.avgPrice)
      .set(currency, model.currency)
      .set(stocked, model.stocked)
      .set(quantityUnit, model.quantityUnit)
      .set(packUnit, model.packUnit)
      .set(stockAccount, model.stockAccount)
      .set(expenseAccount, model.expenseAccount)
      .set(vatCode, model.vatCode)
      .set(company, model.company)
      .where(whereClause(model.id, model.company))
   def buildUpdatePrices(model: Article_) =
    update(articles)
      .set(sprice, model.sprice)
      .set(pprice, model.pprice)
      .set(avgPrice, model.avgPrice)
      .where(whereClause(model.id, model.company))


  override def modify(model: Article): ZIO[Any, RepositoryError, Int]        = {
    val update_ = build(Article_(model))
    ZIO.logInfo(s"Query Update Article is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }
  override def modify(models: List[Article]): ZIO[Any, RepositoryError, Int] = {
    val update_ = models.map(acc => build(Article_(acc)))
    ZIO.foreach(update_.map(renderUpdate))(sql => ZIO.logInfo(s"Query Update Article is ${sql}")) *>
      executeBatchUpdate(update_)
        .provideLayer(driverLayer)
        .map(_.sum)
        .mapError(e => RepositoryError(e.getMessage))
  }


//  override def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Article]] = for {
//    articles <- list(Id).runCollect.map(_.toList)
//  } yield articles
//
//  override def list(Id:(Int,  String)): ZStream[Any, RepositoryError, Article] =
//    ZStream.fromZIO(ZIO.logInfo(s"Query to execute findAll is ${renderRead(SELECT)}")) *>
//      execute(SELECT.where(modelid === Id._1 && company === Id._2)
//        .to { c =>val x = Article.apply(c); map :+ (x); x
//        })
//        .provideDriver(driverLayer)

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Article]]                  =
    list(Id).runCollect.map(_.toList)

  override def list(Id:(Int, String)): ZStream[Any, RepositoryError, Article]                   = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to ( c =>Article.apply(c)))
        .provideDriver(driverLayer)
  }

  override def getBy(Id: (String,  String)): ZIO[Any, RepositoryError, Article]          = {
    val selectAll = SELECT.where(whereClause(Id._1, Id._2))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(c => (Article.apply(c))))
        .findFirst(driverLayer, Id._1)
  }

  override def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Article]] =
    ids.map(id=>getBy((id, company))).flip


  def getByModelId(id: (Int,  String)): ZIO[Any, RepositoryError, List[Article]]= for {
    articles <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield articles
  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Article] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to {c =>
        val x = Article.apply(c); map :+ (x); x
      }
        //selectAll.to(c => (Article.apply(c)))
      )
        .provideDriver(driverLayer)
  }

}

object ArticleRepositoryImpl {
  val live: ZLayer[ConnectionPool, RepositoryError, ArticleRepository] =
    ZLayer.fromFunction(new ArticleRepositoryImpl(_))


}
