package com.kabasoft.iws.repository

import cats._
import cats.effect.Resource
import cats.syntax.all._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, Journal}
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.prelude.FlipOps
import zio.interop.catz._
import zio.{ZIO, _}
import java.time.{Instant, LocalDateTime, ZoneId}


final case  class FinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                   , accRepo: AccountRepository) extends FinancialsTransactionRepository, MasterfileCRUD:

  import FinancialsTransactionRepositorySQL._
  import FinancialsTransactionRepositoryLive.{ sequenceNames, FINANCIAL_SEQUENCE_PREF, FINANCIAL_DETAIL_SEQUENCE_PREF
    , newMasterFilter, oldMasterFilter, master2master, master2Details, Details2Details, newDetailsFilter, details2DeleteFilter, details2UpdateFilter}

  def insertTransact(session: Session[Task], models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
      ZIO.uninterruptibleMask { restore =>
         restore(session.transaction.use { xa =>
            restore(session.prepareR(insert).use { pciMaster =>
              restore(session.prepareR(insertDetails1).use { pciDetails =>
                exec(xa, pciMaster, pciDetails, master2master, master2Details, Details2Details,
                  models, session,
                  sequenceNames(FINANCIAL_SEQUENCE_PREF, FINANCIAL_DETAIL_SEQUENCE_PREF, models)
                ).catchAll { repoError =>ZIO.fail(new Throwable(repoError.message))
              }
            })
        })
      }).mapError(e => RepositoryError(e.getMessage))
    }

  def transact(s: Session[Task], models: List[FinancialsTransaction]): Task[Unit] = {
    def nextId(name: String): ZIO[Any, Throwable, Long] = s.unique(sequenceQuery)(name)
    def withId[A](name: String, f: Long => A): ZIO[Any, Throwable, A] = nextId(name).map(f)

    (for {
      pciMaster <- s.prepareR(insert)
      pcuMaster <- s.prepareR(UPDATE)
      pciDetails <- s.prepareR(insertDetails1)
      pcuDetails <- s.prepareR(UPDATE_DETAILS)
      pcdDetails <- s.prepareR(DELETE_DETAILS)
    } yield (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails)).use {
      case (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails) =>
        // Add new masters if any
        ZIO.foreachDiscard(newMasterFilter(models)) { master =>
          val (masterSeq, detailSeq) = sequenceNames (FINANCIAL_SEQUENCE_PREF, FINANCIAL_DETAIL_SEQUENCE_PREF,models)
          withId(masterSeq, id => master2master(master, id)).flatMap { masterWithId =>
            ZIO.logInfo(s"Insert master: $masterWithId") *>
              pciMaster.execute(masterWithId) *>
              ZIO.foreachDiscard(master2Details(masterWithId, masterWithId.id)) { detail =>
                withId(detailSeq, id => Details2Details(detail, id)).flatMap { detailWithId =>
                  ZIO.logInfo(s"Insert detail: $detailWithId") *>
                    pciDetails.execute(detailWithId)
                }
              }
          }
        }.*>(

        // Update Old masters, if any to
        ZIO.foreachDiscard(oldMasterFilter(models)) { master =>
          val (_, detailSeq) = sequenceNames ( FINANCIAL_SEQUENCE_PREF, FINANCIAL_DETAIL_SEQUENCE_PREF, models)
          pcuMaster.execute(FinancialsTransaction.encodeIt2(master)) *>
            ZIO.foreachDiscard(details2UpdateFilter(master).tapEach( m =>ZIO.logInfo(s"Update old details: $m"))
              .map(FinancialsTransactionDetails.encodeIt2))(pcuDetails.execute) *>
            ZIO.foreachDiscard(newDetailsFilter(master)) { detail =>
              withId(detailSeq, id => Details2Details(detail, id)).flatMap { detailWithId =>
                ZIO.logInfo(s"Insert new details: $detailWithId") *>
                  pciDetails.execute(detailWithId)
              }
            } *>
             // delete lines to delete if any to
            ZIO.foreachDiscard(details2DeleteFilter(master).tapEach( m =>ZIO.logInfo(s"Delete old details: $m"))
               .map(FinancialsTransactionDetails.encodeIt3))(pcdDetails.execute)
        }
        )
    }
  }
  override def create(c: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans <- modify(List(c)).map(_.headOption.getOrElse(FinancialsTransaction.dummy))
    //trans <- create(List(c)).map(_.headOption.getOrElse(FinancialsTransaction.dummy))
       _ <- ZIO.logInfo(s"Inserted : $trans")
    trans2 <- getById(trans.id, trans.modelid, trans.company)
  } yield trans2

  override def create(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = //modify(models)
    for {
      created <- postgres.use { session =>
        (for {
          xa <- session.transaction
          pciMaster <- session.prepareR(insert)
          pciDetails <- session.prepareR(insertDetails1)
          results <- Resource.eval(
            exec(xa, pciMaster, pciDetails, master2master, master2Details, Details2Details,
              models, session,
              FinancialsTransactionRepositoryLive.sequenceNames(
                FinancialsTransactionRepositoryLive.FINANCIAL_SEQUENCE_PREF,
                FinancialsTransactionRepositoryLive.FINANCIAL_DETAIL_SEQUENCE_PREF,
                models
              )).mapError(e => new Throwable(e.message))
          )
        } yield results).use(ZIO.succeed)
      }.mapError(e => RepositoryError(e.getMessage))

      detailed <- ZIO.foreach(created) { result =>
        getById(result.id, result.modelid, result.company)
      }//.map(_.flatten).mapError(e => RepositoryError(e.message))

    } yield detailed

  // Version interne avec Task (pour .use)
  private def transactModifyInternal(s: Session[Task], models: List[FinancialsTransaction]): Task[List[FinancialsTransaction]] = {
    val (seqMaster, seqDetail) = sequenceNames(
      FinancialsTransactionRepositoryLive.FINANCIAL_SEQUENCE_PREF,
      FinancialsTransactionRepositoryLive.FINANCIAL_DETAIL_SEQUENCE_PREF,
      models
    )

    def nextId(company: String): Task[Long] = s.unique(sequenceQuery)(company)

    def exec[A](cmd: PreparedCommand[Task, A], value: A): Task[Unit] = cmd.execute(value).unit

    def withId[A](company: String, f: Long => A): Task[A] = nextId(company).map(f)

    (for {
      pciMaster <- s.prepareR(insert)
      pcuMaster <- s.prepareR(UPDATE)
      pciDetails <- s.prepareR(insertDetails1)
      pcuDetails <- s.prepareR(UPDATE_DETAILS)
      pcdDetails <- s.prepareR(DELETE_DETAILS)
    } yield (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails)).use {
      case (pciMaster, pcuMaster, pciDetails, pcuDetails, pcdDetails) =>
        for {
          newMasters <- ZIO.collectAll(
            newMasterFilter(models).map { master =>
              withId(seqMaster, id => master2master(master, id)).tap { m =>
                ZIO.logInfo(s"Insert new master: $m  ") *>
                exec(pciMaster, m) *>
                  ZIO.foreachDiscard(master2Details(m, m.id)) { d =>
                    ZIO.logInfo(s"Insert new details: $d") *>
                    withId(seqDetail, id => Details2Details(d, id)).tap(exec(pciDetails, _))
                  }
              }
            }
          )

          updatedMasters <- ZIO.collectAll(
            oldMasterFilter(models).map { master =>
              ZIO.succeed(master).tap { m =>
                ZIO.logInfo(s"updating  old master: $m") *>
                exec(pcuMaster, FinancialsTransaction.encodeIt2(m)) *>
                  ZIO.logInfo(s"updating  old details: ${details2UpdateFilter(m)}") *>
                  ZIO.foreachDiscard(details2UpdateFilter(m).map(FinancialsTransactionDetails.encodeIt2))(exec(pcuDetails, _)) *>
                  ZIO.foreachDiscard(newDetailsFilter(m)) { d =>
                    ZIO.logInfo(s"Inserting new details: $d") *>
                    withId(seqDetail, id => Details2Details(d, id)).tap(exec(pciDetails, _))
                  } *>
                  ZIO.logInfo(s"Deleting old details: ") *>
                  ZIO.foreachDiscard(details2DeleteFilter(m).map(FinancialsTransactionDetails.encodeIt3))(exec(pcdDetails, _))
              }
            }
          )
        } yield (newMasters ++ updatedMasters).toList
    }
  }


  // Dans votre méthode modify
  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    postgres.use { session =>
      transactModifyInternal(session, models) // Utilise la version Task
    }.mapError(e => RepositoryError(e.getMessage))


  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] =
    for {
      trans <- modify(List(model)).map(_.headOption.getOrElse(FinancialsTransaction.dummy))
      modified <- getById(trans.id, trans.modelid, trans.company)
    } yield modified

//  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
//    postgres.use { session =>
//      transact(session, models).mapError(e => new Throwable(e.getMessage)) // RepositoryError -> Throwable
//    }.mapError(e => RepositoryError(e.getMessage))
  def listn(p: (List[Int], String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = queryWithTx(postgres, p, ALLn(p._1.size))
  def list(p: (Int, String)):ZIO[Any, RepositoryError, List[FinancialsTransaction]] = queryWithTx(postgres, p, ALL)

  private def  getDetails(p:(Long, String)): ZIO[Any, RepositoryError, List[FinancialsTransactionDetails]] = for {
    details <- queryWithTx(postgres, p, DETAILS1)
    //_ <- ZIO.logInfo(s"Details: $details")
  }yield details

  private def withLines(trans: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    lines_ <- getDetails(trans.id, trans.company)
  } yield trans.copy(lines = if (lines_.nonEmpty) lines_ else List.empty[FinancialsTransactionDetails])

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- list(p)
    transactionsWithDetails     <-  transactions.map(withLines).flip
   // _ <- ZIO.logInfo(s"transactions with Details: $transactionsWithDetails")
  } yield transactionsWithDetails

  override def alln(p: (List[Int], String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- listn(p)
    transactionsWithDetails <- transactions.map(withLines).flip
    // _ <- ZIO.logInfo(s"transactions with Details: $transactionsWithDetails")
  } yield transactionsWithDetails
  
  override def getById(p: (Long, Int, String)):ZIO[Any, RepositoryError, FinancialsTransaction] = for { 
    transaction <- queryWithTxUnique(postgres, p, BY_ID)
    //_ <- ZIO.logInfo(s"transactions : $transaction")
    details <- withLines(transaction)
   // _ <- ZIO.logInfo(s"transactions with Details: $details")
} yield details

  override def getById1(p: (Long, Int, String)): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    transaction <- queryWithTxUnique(postgres, p, BY_ID1)
    details <- withLines(transaction)
  } yield details
  
  override def getBy(ids: List[Long],  modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    details <- transactions.map(withLines).flip
  } yield details

  override def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- queryWithTx(postgres, modelid, BY_MODEL_ID)
    details <- transactions.map(withLines).flip
  } yield details
  
    
  override def getByTransId(p: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction] =for {
    transaction <- queryWithTxUnique(postgres, p, BY_TRANS_ID)
    details <- withLines(transaction)
  } yield details
  

  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, companyId: String, posted:Boolean): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- queryWithTx(postgres, (modelid, companyId, posted, fromPeriod, toPeriod), FIND_4_PERIOD)
    details <-  transactions.map(withLines).flip
  } yield details
  
    
  def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          session.execute(DELETE_ALL)*> session.execute(DELETE_ALL_DETAILS)
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1))


object FinancialsTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & AccountRepository, Throwable, FinancialsTransactionRepository] =
    ZLayer.fromFunction(new FinancialsTransactionRepositoryLive(_, _))
  val FINANCIAL_SEQUENCE_PREF = "master_compta_id_seq"
  val FINANCIAL_DETAIL_SEQUENCE_PREF = "details_compta_id_seq"

   def newMasterFilter(list: List[FinancialsTransaction]): Seq[FinancialsTransaction] = list.filter(_.id === -1L)
   def oldMasterFilter(list: List[FinancialsTransaction]) = list.filter(_.id > 0)
   def master2master(m: FinancialsTransaction, idx: Long) = m.copy(id = idx, id1 = idx)
   def master2Details(m: FinancialsTransaction, idx: Long) = m.lines.map(mx => mx.copy(transid = idx))
   def Details2Details(m: FinancialsTransactionDetails, idx: Long) = m.copy(id = idx)
   def newDetailsFilter(m: FinancialsTransaction) = m.lines.filter(line => line.id === -1L).map(line => line.copy(transid = m.id))
   def details2DeleteFilter(m: FinancialsTransaction) = m.lines.filter(line => line.transid === -2L)
   def details2UpdateFilter(m: FinancialsTransaction) = m.lines.filter(line => line.id > 0 && line.transid === -1L).map(line => line.copy(transid = m.id))
   def setJournalId(m: Journal, idx: Long) = m.copy(id = idx)

  def sequenceNames(prefix1: String, prefix2: String, models: List[FinancialsTransaction]): (String, String) = {
    val model = models.headOption.getOrElse(FinancialsTransaction.dummy)
    val modelid: Int = model.modelid
    val company: String = model.company
    (s"${prefix1}_${company}_${modelid}", s"${prefix2}_${company}")
  }

object FinancialsTransactionRepositorySQL:

  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val financialsTransactionCodec =
    (int8 *: int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: bool *: int4 *: varchar *: varchar *: int4 *: int4)
  private val financialsTransactionCodec4 =
    int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: bool *: int4 *: varchar *: varchar *: int4 *: int4
  private val financialsDetailsTransactionCodec =
    (int8 *: int8 *: varchar *: bool *: varchar *: numeric(12, 2) *: timestamp *: varchar *: varchar *: varchar *: varchar *: varchar)
  private val financialsDetailsTransactionCodec4 =
    (int8 *: varchar *: bool *: varchar *: numeric(12,2) *: timestamp *: varchar *: varchar *: varchar *: varchar *: varchar)

  val mfDecoder: Decoder[FinancialsTransaction] = financialsTransactionCodec.map:
    case (id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content) =>
      FinancialsTransaction(id, oid, id1, costcenter, account, toInstant(transdate), toInstant(enterdate)
        , toInstant(postingdate), period, posted, modelid, company, text, type_journal, file_content)

  val mfEncoder1: Encoder[FinancialsTransaction] = (financialsTransactionCodec.values.contramap(FinancialsTransaction.encodeIt))
  val mfEncoder: Encoder[FinancialsTransaction] = financialsTransactionCodec4.values.contramap(FinancialsTransaction.encodeIt4)
  val detailsEncoder: Encoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.values.contramap(FinancialsTransactionDetails.encodeIt)

  def detailsDecoder: Decoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.map:
      case (id, transid, account, side, oaccount, amount, duedate, text, currency, company, accountName, oaccountName) =>
        FinancialsTransactionDetails(id, transid, account, side, oaccount, amount.bigDecimal, toInstant(duedate), text
        , currency,  company, accountName, oaccountName)

  def base: Fragment[Void]  =
    sql""" SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta """

  val TRANS_ID:Query[Void, Long] = sql"""SELECT NEXTVAL('master_compta_id_seq')""".query(int8)
  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id  IN (${int8.list(nr)}) AND modelid= $int4 AND company = $varchar
           """.query(mfDecoder)
  def BY_IDS(nr: Int): Query[(List[Long], Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id IN (${int8.list(nr)}) AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  def BY_MODEL_ID: Query[(Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE modelid= $int4 AND company = $varchar
           """.query(mfDecoder)

  def BY_MODEL_IDS(nr: Int): Query[(List[Int], String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE modelid IN (${int4.list(nr)}) AND company = $varchar
           """.query(mfDecoder)  

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID1: Query[Long *: Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id1 = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
           
  val ALL: Query[Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
           
  def ALLn (n: Int): Query[List[Int] *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE  modelid IN  (${int4.list(n)}) AND company = $varchar
           """.query(mfDecoder)

  def ALL_DETAILS_ID(lst: List[Long], company:String) = {
    val query: Fragment[Void] =
      sql"""SELECT id, transid, account, side, oaccount, amount,  duedate, text, currency,  company, account_name, oaccount_name
           FROM   details_compta"""
    query(Void) |+|
      AppliedFragment.apply[lst.type](sql" WHERE transid  IN (${int8.list(lst)})", lst) |+|
      sql""" AND company = $varchar""".apply(company)
  }

  def DETAILS(n: Int): Query[List[Long] *: String *: EmptyTuple, FinancialsTransactionDetails] =
    sql"""SELECT id, transid, account, side, oaccount, amount,  duedate, text, currency,  company, account_name, oaccount_name
           FROM   details_compta
           WHERE  transid  IN (${int8.list(n)})  AND company = $varchar
           """.query(detailsDecoder)

  val DETAILS1: Query[Long *: String *: EmptyTuple, FinancialsTransactionDetails] =
    sql"""SELECT id, transid, account, side, oaccount, amount,  duedate, text, currency,  company, account_name, oaccount_name
           FROM   details_compta
           WHERE  transid = $int8  AND company = $varchar
           """.query(detailsDecoder)

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
  val insert: Command[FinancialsTransaction] =
    sql"""INSERT INTO master_compta
         (id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content) VALUES $mfEncoder1 """.command

//  val insertXX: Command[FinancialsTransaction] =
//    sql"""INSERT INTO master_compta
//         (id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content) VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[FinancialsTransaction.TYPE]] =
    sql"""INSERT INTO master_compta 
          (oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content)
         VALUES ${financialsTransactionCodec.values.list(n)}""".command

  val insertDetails1: Command[FinancialsTransactionDetails] =
    sql"""INSERT INTO details_compta (id, transid, account, side, oaccount, amount, duedate, text, currency, company
          , account_name, oaccount_name) VALUES  $detailsEncoder""".command

//  val insertDetails: Command[FinancialsTransactionDetails.D_TYPE4] =
//    sql"""INSERT INTO details_compta (transid, account, side, oaccount, amount, duedate, text, currency, company
//          , account_name, oaccount_name) VALUES  ($financialsDetailsTransactionCodec4)""".command  //  RETURNING id
    
  def insertAllDetails(n:Int): Command[List[FinancialsTransactionDetails.D_TYPE4]] =
    sql"""INSERT INTO details_compta (transid, account, side, oaccount, amount, duedate, text, currency, company
          , account_name, oaccount_name) VALUES (${financialsDetailsTransactionCodec4.values.list(n)})""".command

  val UPDATE: Command[FinancialsTransaction.TYPE2] =
    sql"""UPDATE master_compta
          SET oid = $int8, costcenter = $varchar, account = $varchar, text=$varchar, transdate =$timestamp
          , period=$int4, type_journal = $int4, file_content = $int4
          WHERE id=$int8 and modelid=$int4 and company= $varchar""".command

  val UPDATE_DETAILS: Command[FinancialsTransactionDetails.TYPE2] =
    sql"""UPDATE details_compta
          SET account = $varchar, side = $bool, oaccount = $varchar, amount = $numeric, duedate = $timestamp, text=$varchar, currency = $varchar
          , account_name= $varchar, oaccount_name= $varchar
          WHERE id=$int8 and company= $varchar""".command

  val updatePosted: Command[Long *: Int *: String *: EmptyTuple] =
    sql"""UPDATE master_compta UPDATE SET posted = true
            WHERE id =$int8 AND modelid = $int4 AND  company =$varchar and posted=false
          """.command
  
  val DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM master_compta WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command
  
  val DELETE_ALL: Command[Void] = sql"DELETE FROM master_compta WHERE  company ='-1000'".command

  val DELETE_DETAILS: Command[(Long, String)] = sql"DELETE FROM details_compta WHERE id = $int8 AND company = $varchar".command
  val DELETE_ALL_DETAILS: Command[Void] = sql"DELETE FROM details_compta WHERE company = '-1000'".command

