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

import java.time.{Instant, LocalDateTime, ZoneId}

final case  class TransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                            , accRepo: AccountRepository) extends TransactionRepository, MasterfileCRUD:

  import TransactionRepositorySQL.*

  def transact(s: Session[Task], models: List[Transaction]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciMaster =>
        s.prepareR(insertDetails).use: pciDetails =>
          tryExec(xa, pciMaster, pciDetails, models, models.flatMap(_.lines).map(TransactionDetails.encodeIt))

  def transact(s: Session[Task], models: List[Transaction], oldmodels: List[Transaction]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciMaster =>
        s.prepareR(UPDATE).use: pcuMaster =>
          s.prepareR(insertDetails).use: pciDetails =>
            s.prepareR(UPDATE_DETAILS).use: pcuDetails =>
              tryExec(xa, pciMaster, pciDetails, pcuMaster, pcuDetails
                , models, models.flatMap(_.lines).map(TransactionDetails.encodeIt)
                , oldmodels.map(Transaction.encodeIt2), oldmodels.flatMap(_.lines).map(TransactionDetails.encodeIt2))

  override def create(c:Transaction): ZIO[Any, RepositoryError, Int] = create(List(c))

  override def create(models: List[Transaction]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, models))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)

  override def modify(model: Transaction): ZIO[Any, RepositoryError, Int] = modify(List(model))

  override def modify(models: List[Transaction]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, models))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size) 
  
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
    int8 *: int8 *: int8 *: varchar *: varchar *: timestamptz *: timestamptz *: timestamptz *: int4 *: bool *: int4 *: varchar *: varchar

  private val transactionCodec1 =
    int8 *: int8 *: varchar *: varchar *: timestamptz *: timestamptz *: timestamptz *: int4 *: bool *: int4 *: varchar *: varchar
  private val transactionDetailsCodec =
    int8 *: int8 *: varchar *: varchar *:  numeric(12,2) *: varchar *: numeric(12,2) *: varchar *: timestamp *: varchar *: varchar *: varchar
    //transid, article, article_name, quantity, unit, price, currency, duedate, vat_code
    //          , text, company
  private val transactionDetailsCodec2 =
    int8 *: varchar *: varchar *: numeric(12, 2) *: varchar *: numeric(12, 2) *: varchar *: timestamp *: varchar *: varchar *: varchar  

  val mfDecoder: Decoder[Transaction] = transactionCodec.map:
    case (id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text) =>
      Transaction(id, oid, id1, store, account, transdate.toInstant, enterdate.toInstant, postingdate.toInstant
        , period, posted, modelid, company, text)

  val mfEncoder: Encoder[Transaction] = transactionCodec1.values.contramap(Transaction.encodeIt)
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
    //st.period, st.posted, st.modelid, st.company, st.text

  val insert: Command[Transaction] = sql"INSERT INTO transaction (oid, id1, store, account, enterdate, transdate, postingdate, period, posted, modelid, company, text) VALUES $mfEncoder".command

  def insertAll(n:Int): Command[List[Transaction.TYPE]] = sql"INSERT INTO transaction VALUES ${transactionCodec1.values.list(n)}".command

  val insertDetails: Command[(Long, Long, String, String, BigDecimal, String, BigDecimal, String, LocalDateTime,  String, String, String)] =
    sql"""INSERT INTO transaction_details (id, transid, article, article_name, quantity, unit, price, currency, duedate, vat_code
          , text, company) VALUES $transactionDetailsCodec""".command
    
  def insertAllDetails(n:Int): Command[List[(Long, Long, String, String, BigDecimal, String, BigDecimal, String, LocalDateTime,  String, String, String)]] =
    sql"INSERT INTO transaction_details VALUES ${transactionDetailsCodec.values.list(n)}".command

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

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM transaction WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command
  val DELETE_DETAILS: Command[(Long, String)] = sql"DELETE FROM transaction_details WHERE id = $int8 AND company = $varchar".command