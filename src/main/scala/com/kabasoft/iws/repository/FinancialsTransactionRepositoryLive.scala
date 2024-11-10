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
import com.kabasoft.iws.domain.{ FinancialsTransaction, FinancialsTransactionDetails}
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

import scala.annotation.nowarn

final case  class FinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                                      , accRepo: AccountRepository) 
  extends FinancialsTransactionRepository:

  import FinancialsTransactionRepositorySQL._

  override def create(c: FinancialsTransaction, flag: Boolean): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session
          .prepare(if (flag) upsert else insert)
          .flatMap: cmd =>
            cmd.execute(c).void
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1)

  override def create(list: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session
          .prepare(insertAll(list.length))
          .flatMap: cmd =>
            cmd.execute(list.map(FinancialsTransaction.encodeIt)).void
      .mapBoth(e => RepositoryError(e.getMessage), _ => list.size)

//  private def createDetails(list: List[FinancialsTransactionDetails]): Task[List[FinancialsTransactionDetails]] =
//    postgres
//      .use: session =>
//        session
//         .prepare(insertAllDetails(list.length))
//         .flatMap: cmd =>
//             cmd.execute(list.map(encodeIt))
//                .void
//      .mapBoth(e => RepositoryError(e.getMessage), _ => list)

  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = create(model, true)

  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.size)

  override def all(p: (Int, String)):ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    postgres
      .use: session =>
        session
          .prepare(ALL)
          .flatMap: ps =>
            ps.stream((p._1, p._2), 1024).compile.toList
      .mapBoth(e => RepositoryError(e.getMessage), list => list)

  override def getById(p: (Long, Int, String)):ZIO[Any, RepositoryError, FinancialsTransaction] =
    postgres
      .use: session =>
        session
          .prepare(BY_ID)
          .flatMap(ps => ps.unique(p._1, p._2, p._3))
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  override def getBy(ids: List[Long],  modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    postgres
      .use: session =>
        session
          .prepare(ALL_BY_ID(ids.length))
          .flatMap: ps =>
            ps.stream((ids, modelid, company), chunkSize = 1024)
              .compile
              .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  override def getByIds(ids: List[Long], modelid: Int, companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    postgres
      .use: session =>
        session
         .prepare(BY_IDS(ids.length))
         .flatMap: ps =>
           ps.stream((ids, modelid, companyId), chunkSize = 1024)
              .compile
              .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  override def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    postgres
      .use: session =>
        session
          .prepare(BY_MODEL_ID)
          .flatMap: ps =>
            ps.stream(modelid, chunkSize = 1024)
              .compile
              .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  override def getByTransId(p: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction] =
    postgres
      .use: session =>
        session
          .prepare(BY_TRANS_ID)
          .flatMap(ps => ps.unique(p._1, p._2))
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, companyId: String, posted:Boolean): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    postgres
      .use: session =>
        session
          .prepare(FIND_4_PERIOD)
          .flatMap: ps =>
             ps.stream((modelid, companyId, posted, fromPeriod, toPeriod),  chunkSize = 1024)
                .compile
                .toList
      .mapBoth(e => RepositoryError(e.getMessage), a => a)

  def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session
          .prepare(DELETE)
          .flatMap: cmd =>
            cmd.execute(p._1, p._2, p._3).void
      .mapBoth(e => RepositoryError(e.getMessage), _=> 1)

//  override def post(models: List[FinancialsTransaction], pac2Insert: List[PeriodicAccountBalance], pac2update: U  §§§§IO[List[PeriodicAccountBalance]],
//                    journals: List[Journal]): ZIO[Any, RepositoryError, Int] = for {
//    pac2updatex <- pac2update
//    _ <- ZIO.logInfo(s" New Pacs  to insert into DB ${pac2Insert}")
//    _ <- ZIO.logInfo(s" Old Pacs  to update in DB ${pac2updatex}")
//    _ <- ZIO.logInfo(s" journals  ${journals}")
//    _ <- ZIO.logInfo(s" Transaction posted  ${models}")
//    z = ZIO.when(models.nonEmpty)(updatePostedField4T(models))
//      .zipWith(ZIO.when(pac2Insert.nonEmpty)(createPacs4T(pac2Insert)))((i1, i2) => i1.getOrElse(0) + i2.getOrElse(0))
//      .zipWith(ZIO.when(pac2updatex.nonEmpty)(modifyPacs4T(pac2updatex)))((i1, i2) => i1 + i2.getOrElse(0))
//      .zipWith(ZIO.when(journals.nonEmpty)(createJ4T(journals)))((i1, i2) => i1 + i2.getOrElse(0))
//
//    nr <- transact(z).mapError(e => RepositoryError(e.getMessage)).provideLayer(driverLayer)
//  } yield nr   

object FinancialsTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & AccountRepository, Throwable, FinancialsTransactionRepository] =
    ZLayer.fromFunction(new FinancialsTransactionRepositoryLive(_, _))

object FinancialsTransactionRepositorySQL:
  type D_TYPE=(Long, Long, String, Boolean, String, BigDecimal, LocalDateTime, String, String, String, String)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val financialsTransactionCodec =
    (int8 *: int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: bool *: int4 *: varchar *: varchar *: int4 *: int4)

  private val financialsDetailsTransactionCodec =
    (int8 *: int8 *: varchar *: bool *: varchar *: numeric(12,2) *: timestamp *: varchar *: varchar *: varchar *: varchar )
  
  private[repository] def encodeIt(dt: FinancialsTransactionDetails):D_TYPE =
    (dt.id, dt.transid, dt.account, dt.side, dt.oaccount,  BigDecimal(dt.amount)
      , dt.duedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime
      , dt.text, dt.currency, dt.accountName, dt.oaccountName
   )

  val mfDecoder: Decoder[FinancialsTransaction] = financialsTransactionCodec.map:
    case (id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content) =>
      FinancialsTransaction(id, oid, id1, costcenter, account, toInstant(transdate), toInstant(enterdate)
        , toInstant(postingdate), period, posted, modelid, company, text, type_journal, file_content)

  val mfEncoder: Encoder[FinancialsTransaction] = financialsTransactionCodec.values.contramap(FinancialsTransaction.encodeIt)
  val DetailsEncoder: Encoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.values.contramap(encodeIt)

  def TransactionDetailsDecoder(transId: Long): Decoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.map:
      case (id, transid, account, side, oaccount, amount, duedate, text, currency, accountName, oaccountName) =>
        FinancialsTransactionDetails(id, transid, account, side, oaccount, amount.bigDecimal, toInstant(duedate), text, currency, accountName, oaccountName)

  def base =
    sql""" SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta """

  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id  IN ${int8.list(nr)} AND modelid= $int4 AND company = $varchar
           """.query(mfDecoder)

  def BY_IDS(nr: Int): Query[(List[Long], Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id  IN ${int8.list(nr)} AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  def BY_MODEL_ID: Query[(Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE modelid= $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val FIND_4_PERIOD: Query[Int *: String *: Boolean *: Int *: Int *: EmptyTuple, FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE modelid= $int4 AND company = $varchar AND posted =$bool AND period between $int4  AND $int4 
           """.query(mfDecoder)

  val BY_TRANS_ID: Query[Long *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id= $int8 AND company = $varchar 
           """.query(mfDecoder)  

  val insert: Command[FinancialsTransaction] = sql"""INSERT INTO master_compta VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[FinancialsTransaction.TYPE]] =
    sql"INSERT INTO master_compta VALUES ${financialsTransactionCodec.values.list(n)}".command

  def insertAllDetails(n:Int): Command[List[D_TYPE]] =
    sql"INSERT INTO details_compta VALUES ${financialsDetailsTransactionCodec.values.list(n)}".command
  val upsert: Command[FinancialsTransaction] =
    sql"""INSERT INTO master_compta
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                     = EXCLUDED.id,
           oid                   = EXCLUDED.oid,
           costcenter            = EXCLUDED.costcenter,
            account               = EXCLUDED.account,
            enterdate             = EXCLUDED.enterdate,
            changedate            = EXCLUDED.changedate,
            postingdate           = EXCLUDED.postingdate,
            period               = EXCLUDED.period,
            text                 = EXCLUDED.text,
            type_journal               = EXCLUDED.type_journal,
            file_content               = EXCLUDED.file_content,
            modelid               = EXCLUDED.modelid,
          """.command
    
  val upsertDetails: Command[FinancialsTransactionDetails] =
    sql"""
          INSERT INTO details_compta
           VALUES $DetailsEncoder ON CONFLICT(id, transtid) DO UPDATE SET
           transtid                = EXCLUDED.transtid,
           account                 = EXCLUDED.account,
           side                    = EXCLUDED.side,
            oaccount               = EXCLUDED.oaccount,
            amount                 = EXCLUDED.amount,
            duedate                = EXCLUDED.duedate,
            text                   = EXCLUDED.text,
            currency               = EXCLUDED.currency,
            accountName            = EXCLUDED.accountName,
            oaccountName           = EXCLUDED.oaccountName,
          """.command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM master_compta WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command

  // sql"INSERT INTO Details_compta (id, transid, account, side, oaccount, amount, duedate, text, currency, accountName, oaccountName) VALUES $es".command