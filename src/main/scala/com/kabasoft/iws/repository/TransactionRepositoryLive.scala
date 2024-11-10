package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.ZStream
import zio.stream.interop.fs2z.*
import zio.{Chunk, Task, UIO, ZIO, ZLayer}
import java.time.LocalDate
import com.kabasoft.iws.domain.{ Transaction, TransactionDetails}
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

import scala.annotation.nowarn

final case  class TransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                            , accRepo: AccountRepository) extends TransactionRepository, MasterfileCRUD:

  import TransactionRepositorySQL._

  override def create(c: Transaction, flag: Boolean): ZIO[Any, RepositoryError, Int] = 
    executeWithTx(postgres, c, if (flag) upsert else insert, 1)


  override def create(list: List[Transaction]): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(Transaction.encodeIt), insertAll(list.size), list.size)


  private def createDetails(list: List[TransactionDetails]):ZIO[Any, RepositoryError, List[TransactionDetails]] =
    postgres
      .use: session =>
        session
         .prepare(insertAllDetails(list.length))
         .flatMap: cmd =>
             cmd.execute(list.map(encodeIt)).void
      .mapBoth(e => RepositoryError(e.getMessage), _ => list)

  override def modify(model: Transaction):ZIO[Any, RepositoryError, Int] = create(model, false)

  override def modify(models: List[Transaction]): ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.size)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] =
    postgres
      .use: session =>
        session
          .prepare(ALL)
          .flatMap: ps =>
            ps.stream((p._1, p._2), 1024).compile.toList
      .mapBoth(e => RepositoryError(e.getMessage), list => list)

  def getById(p: (Long, Int, String)): ZIO[Any, RepositoryError, Transaction] =
    postgres
      .use: session =>
        session
          .prepare(BY_ID)
          .flatMap(ps => ps.unique(p._1, p._2, p._3))
      .mapBoth(e => RepositoryError(e.getMessage), a => a)
    
  override def getByModelId( p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] =
    postgres
      .use: session =>
        session
          .prepare(BY_MODEL_ID)
          .flatMap: ps =>
            ps.stream((p._1, p._2), chunkSize = 1024)
              .compile
              .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  override def getByIds(ids: List[Long], modelid: Int, companyId: String): ZIO[Any, RepositoryError, List[Transaction]] =
    postgres
      .use: session =>
        session
          .prepare(ALL_BY_ID(ids.length))
          .flatMap: ps =>
            ps.stream((ids, modelid, companyId), chunkSize = 1024)
              .compile
              .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  override def find4Period(fromPeriod: Int, toPeriod: Int, posted:Boolean, companyId: String): ZIO[Any, RepositoryError, List[Transaction]] =
    postgres
      .use: session =>
        session
         .prepare(BY_PERIOD)
         .flatMap: ps =>
           ps.stream((posted, fromPeriod, toPeriod, companyId), chunkSize = 1024)
             .compile
             .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a) 
    
  override def delete(p:(Long, Int, String)): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session
          .prepare(DELETE)
          .flatMap: cmd =>
            cmd.execute(p._1, p._2, p._3).void
      .mapBoth(e => RepositoryError(e.getMessage), _=> 1)

object TransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & AccountRepository, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryLive(_, _))

private[repository] object TransactionRepositorySQL:
  
  type D_TYPE = (Long, Long, String, String, BigDecimal, String, BigDecimal, String, LocalDateTime, String, String)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val transactionCodec =
    (int8 *: int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: bool *: int4 *: varchar *: varchar)
  private val transactionDetailsCodec =
    (int8 *: int8 *: varchar *: varchar *:  numeric(12,2) *: varchar *: numeric(12,2) *: varchar *: timestamp *: varchar *: varchar )



  private[repository] def encodeIt(dt: TransactionDetails):D_TYPE =
    (dt.id, dt.transid, dt.article, dt.articleName, dt.quantity, dt.unit, dt.price, dt.currency
    , dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime, dt.vatCode, dt.text)

  val mfDecoder: Decoder[Transaction] = transactionCodec.map:
    case (id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text) =>
      Transaction(id, oid, id1, store, account, toInstant(transdate), toInstant(enterdate)
        , toInstant(postingdate), period, posted, modelid, company, text)

  val mfEncoder: Encoder[Transaction] = transactionCodec.values.contramap(Transaction.encodeIt)
  val DetailsEncoder: Encoder[TransactionDetails] = transactionDetailsCodec.values.contramap(encodeIt)

  def detailsDecoder(transId: Long): Decoder[TransactionDetails] = transactionDetailsCodec.map:
      case (id, transid, article, articleName, quantity, unit, price, currency, duedate, vatCode, text) =>
        TransactionDetails(id, transid, article, articleName, quantity.bigDecimal, unit, price.bigDecimal, currency, toInstant(duedate), vatCode, text)

  def base =
    sql""" SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   transaction """

  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   transaction
           WHERE id  IN ${int8.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   transaction
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_MODEL_ID: Query[Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   transaction
           WHERE modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_PERIOD: Query[Boolean *:Int *: Int *:  String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   transaction
           WHERE posted =$bool AND modelid between $int4 AND $int4  AND company = $varchar
           """.query(mfDecoder)  

  val ALL: Query[Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   transaction
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Transaction] = sql"INSERT INTO transaction VALUES $mfEncoder".command

  def insertAll(n:Int): Command[List[Transaction.TYPE]] = sql"INSERT INTO transaction VALUES ${transactionCodec.values.list(n)}".command

  def insertAllDetails(n:Int): Command[List[(Long, Long, String, String, BigDecimal, String, BigDecimal, String, LocalDateTime,  String, String)]] =
    sql"INSERT INTO transaction_details VALUES ${transactionDetailsCodec.values.list(n)}".command
  val upsert: Command[Transaction] =
    sql"""INSERT INTO transaction
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
            id                     = EXCLUDED.id,
            oid                   = EXCLUDED.oid,
            costcenter            = EXCLUDED.store,
            account               = EXCLUDED.account,
            enterdate             = EXCLUDED.enterdate,
            changedate            = EXCLUDED.changedate,
            postingdate           = EXCLUDED.postingdate,
            period               = EXCLUDED.period,
            text                 = EXCLUDED.text,
            type_journal         = EXCLUDED.type_journal,
            file_content         = EXCLUDED.file_content,
            modelid              = EXCLUDED.modelid,
          """.command
    
  val updatePosted: Command[Boolean *: Long *: Int *:  String *: EmptyTuple] =
    sql"""UPDATE transaction
            UPDATE SET
            posted                 = $bool
            WHERE id =$int8 AND modelid = $int4 AND  company =$varchar
          """.command
    
  val upsertDetails: Command[TransactionDetails] =
    sql"""INSERT INTO transaction_compta
           VALUES $DetailsEncoder ON CONFLICT(id, transtid) DO UPDATE SET
            transtid                = EXCLUDED.transtid,
            article                 = EXCLUDED.article,
            articleName             = EXCLUDED.articleName,
            quantity               = EXCLUDED.quantity,
            unit                   = EXCLUDED.unit,
            price                   = EXCLUDED.price,
            currency                = EXCLUDED.currency,
            vat_code                = EXCLUDED.vat_code,
            duedate                = EXCLUDED.duedate,
            text                   = EXCLUDED.text
          """.command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM transaction WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command

//  def insertTransactionDtails(
//                       orderNo: OrderNo,
//                       lineItems: List[TransactionDetails]
//                     ): Command[lineItems.type] =
//    val es = lineItemEncoder(orderNo).list(lineItems)
//    sql"INSERT INTO Details_compta (id, transid, account, side, oaccount, amount, duedate, text, currency, accountName, oaccountName) VALUES $es".command