package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.{Stock, Store, Article}
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{Instant, LocalDateTime, ZoneId}

final case class StoreRepositoryLive(postgres: Resource[Task, Session[Task]], stockRepo:StockRepository
                                     , articleRepo:ArticleRepository) extends StoreRepository, MasterfileCRUD:

  import StoreRepositorySQL._

  override def create(c: Store):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Store]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(encodeIt), insertAll(list.size), list.size)
  override def modify(model: Store):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Store.encodeIt2, UPDATE, 1)
  override def modify(models: List[Store]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Store.encodeIt2)
  private def list(p: (Int, String)): ZIO[Any, RepositoryError, List[Store]] = queryWithTx(postgres, p, ALL)//.debug("LISTTTT")

  override def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Store]] =
    for {
    stores <- list(Id)
    stocks_ <- stockRepo.all(Stock.MODELID, Id._2)
    articles <- articleRepo.getBy(stocks_.map(_.article), Article.MODELID, Id._2)
  } yield stores.map(c => c.copy(stocks = stocks_.filter(_.store == c.id)
    .map(stock =>stock.copy(price = articles.find(_.id == stock.article).getOrElse(Article.dummy).avgPrice))))

  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Store] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Store]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, p, DELETE, 1)
  
object StoreRepositoryLive:

  val live: ZLayer[Resource[Task, Session[Task]] & ArticleRepository &StockRepository, RepositoryError, StoreRepository] =
    ZLayer.fromFunction(new StoreRepositoryLive(_, _, _))

private[repository] object StoreRepositorySQL:

  type TYPE = (String, String, String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4)

  def encodeIt(st: Store): TYPE =
    (
      st.id,
      st.name,
      st.description,
      st.costcenter,
      st.account,
      st.oaccount,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company,
      st.modelid
    )

  val mfDecoder: Decoder[Store] = mfCodec.map:
      case (id, name, description, costcenter, account, oaccount, enterdate, changedate, postingdate, company, modelid) =>
          Store(id, name, description, costcenter, account, oaccount, toInstant(enterdate), toInstant(changedate)
            , toInstant(postingdate), company, modelid)


  val mfEncoder: Encoder[Store] = mfCodec.values.contramap(encodeIt)

  def base =
    sql""" SELECT id, name, description, costcenter, account, oaccount, enterdate, changedate, postingdate
           , company, modelid FROM   store ORDER BY id ASC"""

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Store] =
    sql"""SELECT id, name, description, costcenter, account, oaccount, enterdate, changedate, postingdate, company, modelid
           FROM   store
           WHERE id  IN ( ${varchar.list(nr)}) AND  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Store] =
    sql"""SELECT id, name, description, costcenter,  account, oaccount, enterdate, changedate, postingdate, company, modelid
           FROM   store
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Store] =
    sql"""SELECT id, name, description, costcenter,  account, oaccount, enterdate, changedate, postingdate, company, modelid
           FROM   store
           WHERE  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val insert: Command[Store] = sql"""INSERT INTO store VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[(String, String, String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)]] =
    sql"INSERT INTO store VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Store.TYPE2] =
    sql"""UPDATE store
          SET name = $varchar, description = $varchar, costcenter = $varchar,  account =$varchar, oaccount = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
  

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM store WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
