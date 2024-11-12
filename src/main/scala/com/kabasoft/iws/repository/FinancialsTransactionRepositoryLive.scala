package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case  class FinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                                      , accRepo: AccountRepository)
  extends FinancialsTransactionRepository, MasterfileCRUD:

  import FinancialsTransactionRepositorySQL.*

  override def create(c: FinancialsTransaction, flag: Boolean): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  override def create(list: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(FinancialsTransaction.encodeIt), insertAll(list.size), list.size)

  private def createDetails(list: List[FinancialsTransactionDetails]): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(FinancialsTransactionDetails.encodeIt), insertAllDetails(list.size), list.size)

  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = create(model, true)
  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.size)
  override def all(p: (Int, String)):ZIO[Any, RepositoryError, List[FinancialsTransaction]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Long, Int, String)):ZIO[Any, RepositoryError, FinancialsTransaction] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Long],  modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  override def getByIds(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, (ids, modelid, company), BY_IDS(ids.length))
  override def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, modelid, BY_MODEL_ID)
  override def getByTransId(p: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction] =
    queryWithTxUnique(postgres, p, BY_TRANS_ID)
  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, companyId: String, posted:Boolean): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, (modelid, companyId, posted, fromPeriod, toPeriod), FIND_4_PERIOD)
  def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object FinancialsTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & AccountRepository, Throwable, FinancialsTransactionRepository] =
    ZLayer.fromFunction(new FinancialsTransactionRepositoryLive(_, _))

object FinancialsTransactionRepositorySQL:

  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val financialsTransactionCodec =
    (int8 *: int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: bool *: int4 *: varchar *: varchar *: int4 *: int4)

  private val financialsDetailsTransactionCodec =
    (int8 *: int8 *: varchar *: bool *: varchar *: numeric(12,2) *: timestamp *: varchar *: varchar *: varchar *: varchar )

  val mfDecoder: Decoder[FinancialsTransaction] = financialsTransactionCodec.map:
    case (id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content) =>
      FinancialsTransaction(id, oid, id1, costcenter, account, toInstant(transdate), toInstant(enterdate)
        , toInstant(postingdate), period, posted, modelid, company, text, type_journal, file_content)

  val mfEncoder: Encoder[FinancialsTransaction] = financialsTransactionCodec.values.contramap(FinancialsTransaction.encodeIt)
  val DetailsEncoder: Encoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.values.contramap(FinancialsTransactionDetails.encodeIt)

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

  def insertAllDetails(n:Int): Command[List[FinancialsTransactionDetails.D_TYPE]] =
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
    
  val updatePosted: Command[Long *: Int *: String *: EmptyTuple] =
    sql"""UPDATE master_compta UPDATE SET posted = true
            WHERE id =$int8 AND modelid = $int4 AND  company =$varchar
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