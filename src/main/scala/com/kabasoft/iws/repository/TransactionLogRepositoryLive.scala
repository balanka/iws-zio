package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.TransactionLog
import zio._
import java.time.{Instant, LocalDateTime, ZoneId}

final case class TransactionLogRepositoryLive(postgres: Resource[Task, Session[Task]]) extends TransactionLogRepository, MasterfileCRUD:
  import TransactionLogRepositorySQL.*
  
  def create(item: TransactionLog): ZIO[Any, RepositoryError, Int]= create(List(item))
    //executeWithTx(postgres, item, insert, 1)
  def create(models: List[TransactionLog]): ZIO[Any, RepositoryError, Int]= 
    executeWithTx(postgres, models.map(TransactionLog.encodeIt2), insertAll(models.size), models.size)
  def all(p: (Int, String)): ZIO[Any, RepositoryError, List[TransactionLog]] = queryWithTx(postgres, p, ALL)
  def getById(p: (Long, String)): ZIO[Any, RepositoryError, TransactionLog]= queryWithTxUnique(postgres, p, BY_ID)
  //def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]]
  //def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]] = queryWithTx(postgres, (modelid, company), BY_MODELID)
  def find4StorePeriod(store: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]]=
    queryWithTx(postgres, (store, fromPeriod, toPeriod, company), BY_STORE_PERIOD)

  def find4ArticlePeriod(store: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]] =
    queryWithTx(postgres, (store, fromPeriod, toPeriod, company), BY_ARTICLE_PERIOD)
    
  def find4StoreArticlePeriod(store: String, article: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]] =
    queryWithTx(postgres, (store, article, fromPeriod, toPeriod, company), BY_STORE_ARTICLE_PERIOD)

  override def deleteAll(): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          session.execute(DELETE)
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1))
    

object TransactionLogRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, TransactionLogRepository] =
    ZLayer.fromFunction(new TransactionLogRepositoryLive(_))

object TransactionLogRepositorySQL:
  def toInstant(localDateTime: LocalDateTime): Instant = localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
  val mfCodec =
    (int8 *: int8 *: int8 *:int8 *: varchar *: varchar *: varchar *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: varchar *: numeric(12, 2) *: numeric(12, 2) *: varchar *: timestamp *: varchar *: timestamp*: timestamp*: timestamp *: int4 *:  varchar *: int4)
  val mfCodec2 =
    (int8 *: int8 *:int8 *: varchar *: varchar *: varchar *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: varchar *: numeric(12, 2) *: numeric(12, 2) *: varchar *: timestamp *: varchar *: timestamp*: timestamp*: timestamp *: int4 *:  varchar *: int4)

  val mfEncoder: Encoder[TransactionLog] = mfCodec.values.contramap(TransactionLog.encodeIt)
  val mfEncoder2: Encoder[TransactionLog] = mfCodec2.values.contramap(TransactionLog.encodeIt2)

  val mfDecoder: Decoder[TransactionLog] = mfCodec.map:
    case (id, id1, transid, oid, store, account, article, quantity, stock, wholeStock, unit, price, avgPrice, currency
    , duedate, text, transdate, postingdate, enterdate, period, company, modelid) =>
      TransactionLog(id, id1, transid, oid, store, account, article, quantity.bigDecimal, stock.bigDecimal, wholeStock.bigDecimal, unit, price.bigDecimal, avgPrice.bigDecimal
        , currency, toInstant(duedate), text, toInstant(transdate), toInstant(postingdate), toInstant(enterdate), period, company, modelid)
  
  val FIND_4_STORE_PERIOD_QUERY: Query[String  *: Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
    , duedate, text, transdate, postingdate, enterdate, period, company, modelid
      FROM transaction_log
       WHERE store=$varchar AND period between  $int4 and  $int4 AND  company =$varchar
       .orderBy(article.descending, period.descending)
       """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: String *: EmptyTuple, TransactionLog] =
    sql"""id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE id = $int8  AND company = $varchar
           """.query(mfDecoder)

  val BY_MODELID: Query[Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_STORE_PERIOD: Query[String *:Int *: Int *:String *: EmptyTuple, TransactionLog] =
    sql"""id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE store =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ARTICLE_PERIOD: Query[String *: Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE article =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_STORE_ARTICLE_PERIOD: Query[String *: String *:Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE store =$varchar AND  article =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val insert: Command[TransactionLog] = 
    sql"""INSERT INTO transaction_log (id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit
                            ,  price, avg_price, currency, duedate, text, transdate, postingdate, enterdate
                            , period, company, modelid) VALUES $mfEncoder2""".stripMargin.command

  def insertAll(n: Int): Command[List[TransactionLog.TYPE2]] =
    sql"""INSERT INTO transaction_log (id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit
          ,  price, avg_price, currency, duedate, text, transdate, postingdate, enterdate, period, company, modelid) 
          VALUES ${mfCodec2.values.list(n)}""".command

  def DELETE: Command[Void] =
     sql"""DELETE FROM transaction_log WHERE company = '-1000'""".command