package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.interop.catz.*
import zio.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Transaction, TransactionDetails, TransactionLog, common}
import com.kabasoft.iws.repository.TransactionRepositoryLive.{ newTransactionDetailsFilter, newTransactionFilter
  , oldTransactionFilter, sequenceNames, setDetailsId, setTransactionId, transaction2Details
  , transactionDetails2DeleteFilter, transactionDetails2UpdateFilter}

import java.time.{Instant, LocalDateTime, ZoneId}

final case  class TransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                            , accRepo: AccountRepository
                                            , articleRepo: ArticleRepository) extends TransactionRepository, MasterfileCRUD:

  import TransactionRepositorySQL._

  override def create(model: Transaction): ZIO[Any, RepositoryError, Transaction] = modify(model)
  override def create(models: List[Transaction]): ZIO[Any, RepositoryError, List[Transaction]] = modify(models)
  override def modify(model: Transaction): ZIO[Any, RepositoryError, Transaction] =
    for {
      trans <- modify(List(model)).map(_.headOption.getOrElse(Transaction.dummy))
      modified <- getById(trans.id, trans.modelid, trans.company)
    } yield modified

  override def modify(models: List[Transaction]): ZIO[Any, RepositoryError, List[Transaction]] =
    postgres.use { session =>
      transactModifyInternal(session, models)
    }.mapError(e => RepositoryError(e.getMessage))

  private def transactModifyInternal(s: Session[Task], models: List[Transaction]): Task[List[Transaction]] = {
    val (seqMaster, seqDetail) = sequenceNames(
      TransactionRepositoryLive.TRANSACTION_SEQUENCE_PREF,
      TransactionRepositoryLive.TRANSACTION_DETAIL_SEQUENCE_PREF,
      models
    )

    def nextId(company: String): Task[Long] = s.unique(sequenceQuery)(company)

    def exec[A](cmd: PreparedCommand[Task, A], value: A): Task[Unit] = cmd.execute(value).unit

    def withId[A](company: String, f: Long => A): Task[A] = nextId(company).map(f)

    (for {
      pciMaster <- s.prepareR(insert)
      pcuMaster <- s.prepareR(UPDATE)
      pciDetails <- s.prepareR(insertDetails)
      pcuDetails <- s.prepareR(UPDATE_DETAILS)
      pcdDetails <- s.prepareR(DELETE_DETAILS)
    } yield (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails)).use {
      case (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails) =>
        for {
          newMasters <- ZIO.collectAll(
            newTransactionFilter(models).map { master =>
              withId(seqMaster, id => setTransactionId(master, id)).tap { m =>
                ZIO.logInfo(s"Insert new master: $m  ") *>
                  exec(pciMaster, m) *>
                  ZIO.foreachDiscard(transaction2Details(m, m.id)) { d =>
                    ZIO.logInfo(s"Insert new details: $d") *>
                      withId(seqDetail, id => setDetailsId(d, id)).tap(exec(pciDetails, _))
                  }
              }
            }
          )

          updatedMasters <- ZIO.collectAll(
            oldTransactionFilter(models).map { master =>
              ZIO.succeed(master).tap { m =>
                ZIO.logInfo(s"updating  old master: $m") *>
                  exec(pcuMaster, Transaction.encodeIt2(m)) *>
                  ZIO.logInfo(s"updating  old details: ${transactionDetails2UpdateFilter(m)}") *>
                  ZIO.foreachDiscard(transactionDetails2UpdateFilter(m).map(TransactionDetails.encodeIt2))(exec(pcuDetails, _)) *>
                  ZIO.foreachDiscard(newTransactionDetailsFilter(m)) { d =>
                    ZIO.logInfo(s"Inserting new details: $d") *>
                      withId(seqDetail, id => setDetailsId(d, id)).tap(exec(pciDetails, _))
                  } *>
                  ZIO.logInfo(s"Deleting old details: ") *>
                  ZIO.foreachDiscard(transactionDetails2DeleteFilter(m).map(TransactionDetails.encodeIt3))(exec(pcdDetails, _))
              }
            }
          )
        } yield newMasters ++ updatedMasters
    }
  }
  override def getById(p: (Long, Int, String)): ZIO[Any, RepositoryError, Transaction] = for {
    transaction <- queryWithTxUnique(postgres, p, BY_ID)
    details <- withLines(transaction)
  } yield details
  
  override def getById1(p: (Long, Int, String)): ZIO[Any, RepositoryError, Transaction] = for {
    transaction <- queryWithTxUnique(postgres, p, BY_ID1)
    details <- withLines(transaction)
  } yield details
    
  override def getByModelId( p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = for {
    transactions <- queryWithTx(postgres, p, BY_MODEL_ID)
    details <- transactions.map(withLines).flip
  } yield details
    
  override def getByIds(ids: List[Long], modelid: Int, companyId: String): ZIO[Any, RepositoryError, List[Transaction]] = for {
    transactions <- queryWithTx(postgres, (ids, modelid, companyId), ALL_BY_ID(ids.length))
    details <- transactions.map(withLines).flip
  } yield details
    
  
  def list(p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = queryWithTx(postgres, p, ALL)
  private def getDetails(p: (Long, String)): ZIO[Any, RepositoryError, List[TransactionDetails]] = queryWithTx(postgres, p, DETAILS1)

  private def getByTransId1(trans: Transaction): ZIO[Any, RepositoryError, Transaction] = for {
    lines_ <- getDetails(trans.id1, trans.company)
  } yield trans.copy(lines = lines_) 

  private def withLines(trans: Transaction): ZIO[Any, RepositoryError, Transaction] =
    getByTransId1(trans)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = for {
    transactions <- list(p)
    details <- transactions.map(withLines).flip
  } yield details

  override def find4Period(fromPeriod: Int, toPeriod: Int, posted:Boolean, companyId: String): ZIO[Any, RepositoryError, List[Transaction]] =
    queryWithTx(postgres, (posted, fromPeriod, toPeriod, companyId), BY_PERIOD)
  override def delete(p:(Long, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          session.execute(DELETE_All)*> session.execute(DELETE_ALL_DETAILS)
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1))


object TransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & AccountRepository& ArticleRepository, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryLive(_, _, _))
  val TRANSACTION_SEQUENCE_PREF = "transaction_id_seq"
  val TRANSACTION_DETAIL_SEQUENCE_PREF = "transaction_details_id_seq"
  val TRANSACTION_LOG_SEQUENCE_PREF = "transaction_log_id_seq"

  def sequenceNames(prefix1: String, prefix2: String, models: List[Transaction]): (String, String) = {
    val model = models.headOption.getOrElse(Transaction.dummy)
    val modelid: Int = model.modelid
    val company: String = model.company
    (s"${prefix1}_${company}_${modelid}", s"${prefix2}_${company}")
  }

  def sequenceName(prefix: String,  models: List[TransactionLog]) = {
    val company: String = models.headOption.getOrElse(TransactionLog.dummy).company
    s"${prefix}_$company"
  }

   def newTransactionFilter(list: List[Transaction]) = list.filter(_.id === -1L)
   def oldTransactionFilter(list: List[Transaction]) = list.filter(_.id > 0)
   def setTransactionId(m: Transaction, idx: Long) = m.copy(id = idx, id1 = idx)
   def transaction2Details(m: Transaction, idx: Long) = m.lines.map(_.copy(transid = idx))
   def setDetailsId(m: TransactionDetails, idx: Long) = m.copy(id = idx)
   def newTransactionDetailsFilter(m: Transaction) = m.lines.filter(_.id === -1L).map(line => line.copy(transid = m.id))
   def transactionDetails2DeleteFilter(m: Transaction) = m.lines.filter(_.transid === -2L)
   def transactionDetails2UpdateFilter(m: Transaction) = m.lines.filter(line => line.id > 0 && line.transid === -1L)
                                                          .map(line => line.copy(transid = m.id))
  def setTransactionLogId(m: TransactionLog, idx: Long) = m.copy(id = idx)

private[repository] object TransactionRepositorySQL:
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
  private val transactionCodec =
    int8 *: int8 *: int8 *: varchar *: varchar *: timestamptz *: timestamptz *: timestamptz *: int4 *: bool *: int4 *: varchar *: varchar*: varchar

//  private val transactionCodec1 =
//    int8 *: int8 *: varchar *: varchar *: timestamptz *: timestamptz *: timestamptz *: int4 *: bool *: int4 *: varchar *: varchar*: varchar
  private val transactionDetailsCodec =
    int8 *: int8 *: varchar *: varchar *:  numeric(12,2) *: varchar *: numeric(12,2) *: varchar *: timestamp *: varchar *: numeric(12, 2) *: varchar *: varchar

//  private val transactionDetailsCodec2 =
//    int8 *: varchar *: varchar *: numeric(12, 2) *: varchar *: numeric(12, 2) *: varchar *: timestamp *: varchar *: numeric(12, 2) *: varchar *: varchar

  val mfDecoder: Decoder[Transaction] = transactionCodec.map:
    case (id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, footText) =>
      Transaction(id, oid, id1, store, account, transdate.toInstant, enterdate.toInstant, postingdate.toInstant
        , period, posted, modelid, company, text, footText )

  val mfEncoder: Encoder[Transaction] = transactionCodec.values.contramap(Transaction.encodeIt)
  val detailsEncoder: Encoder[TransactionDetails] = transactionDetailsCodec.values.contramap(TransactionDetails.encodeIt)

  val detailsDecoder: Decoder[TransactionDetails] = transactionDetailsCodec.map:
      case (id, transid, article, articleName, quantity, unit, price, currency, duedate, vatCode, vat, text, company) =>
        TransactionDetails(id, transid, article, articleName, quantity.bigDecimal, unit, price.bigDecimal, currency, toInstant(duedate), vatCode, vat.bigDecimal, text, company)

  def base =
    sql""" SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, foot_text
           FROM   transaction """

  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, foot_text
           FROM   transaction
           WHERE id  IN (${int8.list(nr)}) AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, foot_text
           FROM   transaction
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
           
  val BY_ID1: Query[Long *: Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, foot_text
           FROM   transaction
           WHERE id1 = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_MODEL_ID: Query[Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, foot_text
           FROM   transaction
           WHERE modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_PERIOD: Query[Boolean *:Int *: Int *:  String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, foot_text
           FROM   transaction
           WHERE posted =$bool AND modelid between $int4 AND $int4  AND company = $varchar
           """.query(mfDecoder)  

  val ALL: Query[Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, foot_text
           FROM   transaction
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val DETAILS1: Query[Long *: String *: EmptyTuple, TransactionDetails] =
    sql"""SELECT id, transid, article, article_name, quantity, unit, price, currency, duedate, vat_code, vat, text, company
           FROM   transaction_details
           WHERE  transid = $int8  AND company = $varchar
           """.query(detailsDecoder)
           
//  val copy_insert: Command[Int *: Long *:  String *: EmptyTuple] =
//    sql"""INSERT INTO transaction (oid, id1, store, account, enterdate, transdate, postingdate, period, posted, modelid
//         , company, text) 
//          SELECT (id, 0, store, account, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, period, false, $int2
//         , company, text  FROM transaction 
//         WHERE id = $int4  AND company = $varchar """.command
         
  val insert: Command[Transaction] =
    sql"""INSERT INTO transaction (id, oid, id1, store, account, enterdate, transdate, postingdate, period, posted, modelid
         , company, text, foot_text) VALUES $mfEncoder""".command

//  def insertAll(n:Int): Command[List[Transaction.TYPE]] = 
//    sql"""INSERT INTO transaction (oid, id1, store, account, enterdate, transdate, postingdate, period, posted, modelid
//         , company, text, foot_text) VALUES ${transactionCodec1.values.list(n)}""".command

  val insertDetails: Command[TransactionDetails] =
    sql"""INSERT INTO transaction_details (id, transid, article, article_name, quantity, unit, price, currency, duedate, vat_code
          , vat, text, company) VALUES $detailsEncoder """.command

//  val insertDetails: Command[TransactionDetails.D_TYPE1] =
//    sql"""INSERT INTO transaction_details (transid, article, article_name, quantity, unit, price, currency, duedate, vat_code
//          , vat, text, company) VALUES ($transactionDetailsCodec2 )""".command
    
//  def insertAllDetails(n:Int): Command[List[TransactionDetails.D_TYPE1]] =
//    sql"""INSERT INTO transaction_details (transid, article, article_name, quantity, unit, price, currency
//          , duedate, vat_code, vat, text, company) VALUES ${transactionDetailsCodec.values.list(n)}""".stripMargin.command

  val updatePosted: Command[Long *: Int *:  String *: EmptyTuple] =
    sql"""UPDATE transaction UPDATE SET posted = true
            WHERE id =$int8 AND modelid = $int4 AND  company =$varchar
          """.command

  val UPDATE: Command[Transaction.TYPE2] =
    sql"""UPDATE transaction
          SET oid = $int8, store = $varchar, account = $varchar, transdate = $timestamp, text=$varchar, foot_text=$varchar
          WHERE id=$int8 and modelid=$int4 and company= $varchar""".command

  val UPDATE_DETAILS: Command[TransactionDetails.TYPE2] =
    sql"""UPDATE transaction_details
          SET transid=$int8, article = $varchar, quantity = $numeric, unit = $varchar, price = $numeric, currency = $varchar
          , duedate = $timestamp, text=$varchar, article_name = $varchar, vat_code = $varchar, vat = $numeric
          WHERE id=$int8 and company= $varchar""".command

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM transaction WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command

  def DELETE_All: Command[Void] = sql"DELETE FROM transaction WHERE  company = '-1000'".command
  val DELETE_DETAILS : Command[(Long, String)] = sql"DELETE FROM transaction_details WHERE id = $int8 AND company = $varchar".command
  val DELETE_ALL_DETAILS: Command[Void] = sql"DELETE FROM transaction_details WHERE  id=-2 and company = '-1000'".command
  val NEXT_ID:Query[Void, Long] = sql"SELECT NEXTVAL('master_compta_id_seq')".query(int8)
  //id  IN (${varchar.list(nr)})