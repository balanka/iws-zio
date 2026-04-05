package com.kabasoft.iws.repository

import TransactionLogRepositorySQL.*
import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.TransactionLog
import zio.*

import java.time.{Instant, LocalDateTime, ZoneId}

final case class TransactionLogRepositoryLive(postgres: Resource[Task, Session[Task]]) extends TransactionLogRepository, MasterfileCRUD:
  import TransactionLogRepositorySQL.*

  val TRANSACTION_LOG_SEQUENCE_PREF = "transaction_log_id_seq"
 
  private def sequenceName(prefix: String, models: List[TransactionLog]) = 
    val company: String = models.headOption.getOrElse(TransactionLog.dummy).company
    s"${prefix}_$company"
  
  private def master2master(m: TransactionLog, idx: Long) = m.copy(id = idx, id1 = idx)

  def transact (session: Session[Task],  models: List[TransactionLog], sequenceName:String): Task[Unit] =
    session.transaction.use: xa =>
      session.prepareR(insert).use: pci =>
         tryExec(xa, session, pci, master2master,  models, sequenceName)
  
  def create(item: TransactionLog): ZIO[Any, RepositoryError, Int]= create(List(item))
  def create(models: List[TransactionLog]): ZIO[Any, RepositoryError, Int]=
    postgres
      .use:
          session =>
            transact(session, models, sequenceName(TRANSACTION_LOG_SEQUENCE_PREF, models))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.size)   

  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[TransactionLog]] =
    queryWithTx(postgres, (fromPeriod, toPeriod, company), BY_PERIOD)
    
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
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
    , duedate, text, transdate, postingdate, enterdate, period, company, modelid
      FROM transaction_log
       WHERE store=$varchar AND period between  $int4 and  $int4 AND  company =$varchar
       .orderBy(article.descending, period.descending)
       """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, TransactionLog] =
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: String *: EmptyTuple, TransactionLog] =
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE id = $int8  AND company = $varchar
           """.query(mfDecoder)

  val BY_MODELID: Query[Int *: String *: EmptyTuple, TransactionLog] =
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_PERIOD: Query[Int *: Int *:String *: EmptyTuple, TransactionLog] =
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE  period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)
             
  val BY_STORE_PERIOD: Query[String *:Int *: Int *:String *: EmptyTuple, TransactionLog] =
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE store =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ARTICLE_PERIOD: Query[String *: Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
       , duedate, text, transdate, postingdate, enterdate, period, company, modelid
         FROM transaction_log
           WHERE article =$varchar AND period between $int4 AND $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_STORE_ARTICLE_PERIOD: Query[String *: String *:Int *: Int *: String *: EmptyTuple, TransactionLog] =
    sql"""SELECT id, id1, transid, oid, store, account, article, quantity, stock, whole_stock, unit, price, avg_price, currency
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