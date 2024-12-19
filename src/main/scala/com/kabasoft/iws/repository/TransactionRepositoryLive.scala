package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Transaction, TransactionDetails}
import com.kabasoft.iws.repository.MasterfileCRUD.{UpdateCommand, InsertBatch, ExecCommand}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer, *}
import zio.interop.catz.*
//import zio.{ZIO, *}

import java.time.{Instant, LocalDateTime, ZoneId}

final case  class TransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                            , accRepo: AccountRepository) extends TransactionRepository, MasterfileCRUD:

  import TransactionRepositorySQL.*

  override def create(c: Transaction): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Transaction]): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(Transaction.encodeIt), insertAll(list.size), list.size)

  private def createDetails(list: List[TransactionDetails]):ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(TransactionDetails.encodeIt), insertAllDetails(list.size), list.size)
  
  override def modify(model: Transaction):ZIO[Any, RepositoryError, Int] = modify(List(model))
  override def modify(models: List[Transaction]): ZIO[Any, RepositoryError, Int] = {
    val newLines = models.flatMap(ftr => ftr.lines.filter(_.id == -1L).map(l => l.copy(transid = ftr.id)))
    val deletedLine = models.flatMap(ftr => ftr.lines.filter(_.transid == -2L))
    val deletedLineIds = deletedLine.map(line => line.id)
    val oldLines2Update = models.flatMap(ftr => ftr.lines.filter(_.id > 0L).map(l => l.copy(transid = ftr.id)))
    val insertDetailsCmd = TransactionRepositorySQL.insertAllDetails(newLines.length)
    val updateDetailsCmd = TransactionRepositorySQL.UPDATE_DETAILS
    val createDetailsCmd = InsertBatch(newLines, TransactionDetails.encodeIt, insertDetailsCmd)
    val updateDetailsCmds = ExecCommand(oldLines2Update, TransactionDetails.encodeIt2, updateDetailsCmd)
    val deleteDetailsCmd = ExecCommand(oldLines2Update, TransactionDetails.encodeIt3, DELETE_DETAILS)
    val updateFtrCmd = models.map(ftr => UpdateCommand(ftr, Transaction.encodeIt2, TransactionRepositorySQL.UPDATE))
    executeBatchWithTx2(postgres, updateFtrCmd, List.empty, List(deleteDetailsCmd), List(createDetailsCmd), List(updateDetailsCmds))
    ZIO.succeed(newLines.size + oldLines2Update.size + deletedLine.size + 1)
  }
  //override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Long, Int, String)): ZIO[Any, RepositoryError, Transaction] = queryWithTxUnique(postgres, p, BY_ID)
  override def getByModelId( p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = queryWithTx(postgres, p, BY_MODEL_ID)
  override def getByIds(ids: List[Long], modelid: Int, companyId: String): ZIO[Any, RepositoryError, List[Transaction]] =
    queryWithTx(postgres, (ids, modelid, companyId), ALL_BY_ID(ids.length))

  def list(p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = queryWithTx(postgres, p, ALL)
  private def getDetails(p: (Long, String)): ZIO[Any, RepositoryError, List[TransactionDetails]] = queryWithTx(postgres, p, DETAILS1)

  private def getByTransId1(trans: Transaction): ZIO[Any, RepositoryError, Transaction] = for {
    lines_ <- getDetails(trans.id1, trans.company)
  } yield trans.copy(lines = if (lines_.nonEmpty) lines_ else List.empty[TransactionDetails])

  private def withLines(trans: Transaction): ZIO[Any, RepositoryError, Transaction] =
    getByTransId1(trans)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = for {
    transactions <- list(p)
    details <- transactions.map(withLines).flip
  } yield details

  override def find4Period(fromPeriod: Int, toPeriod: Int, posted:Boolean, companyId: String): ZIO[Any, RepositoryError, List[Transaction]] =
    queryWithTx(postgres, (posted, fromPeriod, toPeriod, companyId), BY_PERIOD)
  override def delete(p:(Long, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object TransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & AccountRepository, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryLive(_, _))

private[repository] object TransactionRepositorySQL:
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val transactionCodec =
    (int8 *: int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: bool *: int4 *: varchar *: varchar)
  private val transactionDetailsCodec =
    (int8 *: int8 *: varchar *: varchar *:  numeric(12,2) *: varchar *: numeric(12,2) *: varchar *: timestamp *: varchar *: varchar *: varchar )

  val mfDecoder: Decoder[Transaction] = transactionCodec.map:
    case (id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text) =>
      Transaction(id, oid, id1, store, account, toInstant(transdate), toInstant(enterdate)
        , toInstant(postingdate), period, posted, modelid, company, text)

  val mfEncoder: Encoder[Transaction] = transactionCodec.values.contramap(Transaction.encodeIt)
  val DetailsEncoder: Encoder[TransactionDetails] = transactionDetailsCodec.values.contramap(TransactionDetails.encodeIt)

  val detailsDecoder: Decoder[TransactionDetails] = transactionDetailsCodec.map:
      case (id, transid, article, articleName, quantity, unit, price, currency, duedate, vatCode, text, company) =>
        TransactionDetails(id, transid, article, articleName, quantity.bigDecimal, unit, price.bigDecimal, currency, toInstant(duedate), vatCode, text, company)

  def base =
    sql""" SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction """

  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction
           WHERE id  IN ${int8.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_MODEL_ID: Query[Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction
           WHERE modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_PERIOD: Query[Boolean *:Int *: Int *:  String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction
           WHERE posted =$bool AND modelid between $int4 AND $int4  AND company = $varchar
           """.query(mfDecoder)  

  val ALL: Query[Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val DETAILS1: Query[Long *: String *: EmptyTuple, TransactionDetails] =
    sql"""SELECT id, transid, article, article_name, quantity, unit, price, currency, duedate, vat_code, text, company
           FROM   transaction_details
           WHERE  transid = $int8  AND company = $varchar
           """.query(detailsDecoder)

  val insert: Command[Transaction] = sql"INSERT INTO transaction VALUES $mfEncoder".command

  def insertAll(n:Int): Command[List[Transaction.TYPE]] = sql"INSERT INTO transaction VALUES ${transactionCodec.values.list(n)}".command

  def insertAllDetails(n:Int): Command[List[(Long, Long, String, String, BigDecimal, String, BigDecimal, String, LocalDateTime,  String, String, String)]] =
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
            modelid              = EXCLUDED.modelid,
          """.command
    
  val updatePosted: Command[Long *: Int *:  String *: EmptyTuple] =
    sql"""UPDATE transaction UPDATE SET posted = true
            WHERE id =$int8 AND modelid = $int4 AND  company =$varchar
          """.command

  val UPDATE: Command[Transaction.TYPE2] =
    sql"""UPDATE transaction
          SET oid = $int8, store = $varchar, account = $varchar, transdate = $timestamp, text=$varchar
          WHERE id=$int8 and modelid=$int4 and company= $varchar""".command

  val UPDATE_DETAILS: Command[TransactionDetails.TYPE2] =
    sql"""UPDATE transaction_details
          SET transid=$int8, article = $varchar, quantity = $numeric, unit = $varchar, price = $numeric, currency = $varchar
          , duedate = $timestamp, text=$varchar, article_name = $varchar, article_nvat_codeame = $varchar
          WHERE id=$int8 and company= $varchar""".command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM transaction WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command
  val DELETE_DETAILS: Command[(Long, String)] = sql"DELETE FROM transaction_details WHERE id = $int8 AND company = $varchar".command