package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.ZStream
import zio.stream.interop.fs2z.*
import zio.{Chunk, Task, UIO, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.TransactionLog
import zio.*
import zio.stream.*

import java.time.{Instant, LocalDateTime, ZoneId}

final case class TransactionLogRepositoryLive(postgres: Resource[Task, Session[Task]]) extends TransactionLogRepository, MasterfileCRUD:
  import TransactionLogRepositorySQL.*
  
  def create(item: TransactionLog): ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, item, insert, 1)
  def create(models: List[TransactionLog]): ZIO[Any, RepositoryError, Int]= 
    executeWithTx(postgres, models.map(TransactionLog.encodeIt), insertAll(models.size), models.size)
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

    

object TransactionLogRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, TransactionLogRepository] =
    ZLayer.fromFunction(new TransactionLogRepositoryLive(_))

object TransactionLogRepositorySQL:
  def toInstant(localDateTime: LocalDateTime): Instant = localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
  val mfCodec =
    (int8 *: int8 *: int8 *: varchar *: varchar *: varchar *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: varchar *: numeric(12, 2) *: numeric(12, 2) *: varchar *: timestamp *: varchar *: timestamp*: timestamp*: timestamp *: int4 *:  varchar *: int4)

  val mfEncoder: Encoder[TransactionLog] = mfCodec.values.contramap(TransactionLog.encodeIt)

  val mfDecoder: Decoder[TransactionLog] = mfCodec.map:
    case (id, transid, oid, store, account, article, quantity, stock, wholeStock, unit, price, avgPrice, currency
    , duedate, text, transdate, postingdate, enterdate, period, company, modelid) =>
      TransactionLog(id, transid, oid, store, account, article, quantity.bigDecimal, stock.bigDecimal, wholeStock.bigDecimal, unit, price.bigDecimal, avgPrice.bigDecimal
        , currency, toInstant(duedate), text, toInstant(transdate), toInstant(postingdate), toInstant(enterdate), period, company, modelid)
  
  val FIND_4_STORE_PERIOD_QUERY: Query[String  *: Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
    , duedate, text, transdate, postingdate, enterdate, period, company, modelid
      FROM transaction_log
       WHERE store=$varchar AND period between  $int4 and  $int4 AND  company =$varchar
       .orderBy(article.descending, period.descending)
       """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: String *: EmptyTuple, TransactionLog] =
    sql"""id, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE id = $int8  AND company = $varchar
           """.query(mfDecoder)

  val BY_MODELID: Query[Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_STORE_PERIOD: Query[String *:Int *: Int *:String *: EmptyTuple, TransactionLog] =
    sql"""id, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE store =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ARTICLE_PERIOD: Query[String *: Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE article =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_STORE_ARTICLE_PERIOD: Query[String *: String *:Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""id, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE store =$varchar AND  article =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val insert: Command[TransactionLog] = sql"""INSERT INTO transaction_log VALUES $mfEncoder """.command

  def insertAll(n: Int): Command[List[TransactionLog.TYPE]] =
    sql"INSERT INTO transaction_log VALUES ${mfCodec.values.list(n)}".command
