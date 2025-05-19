package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, common}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.interop.catz.*
import zio.{ZIO, *}

import java.time.{Instant, LocalDateTime, ZoneId}


final case  class FinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                                      , accRepo: AccountRepository) extends FinancialsTransactionRepository, MasterfileCRUD:

  import FinancialsTransactionRepositorySQL._
  
  def buildId(transactions: List[FinancialsTransaction]): List[FinancialsTransaction] =
    transactions.zipWithIndex.map { case (ftr, i) =>
      val idx_ = Instant.now().getNano + i.toLong
      val idx = if(ftr.id1 >0) ftr.id1 else idx_
      ftr.copy(id1 = idx, lines = ftr.lines.map(_.copy(transid = idx)), period = common.getPeriod(ftr.transdate))
    }
  
  def transact(s: Session[Task], models: List[FinancialsTransaction]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciMaster =>
        s.prepareR(insertDetails).use: pciDetails =>
          tryExec(xa, pciMaster, pciDetails, models, models.flatMap(_.lines).map(FinancialsTransactionDetails.encodeIt4))
          
  
  // session, List.empty, newLine2Insert, models, oldLines2Update,  oldLine2Delete)
  def transact(s: Session[Task], models: List[FinancialsTransaction], oldmodels: List[FinancialsTransaction]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciMaster =>
        s.prepareR(UPDATE).use: pcuMaster =>
          s.prepareR(insertDetails).use: pciDetails =>
            s.prepareR(UPDATE_DETAILS).use: pcuDetails =>
               tryExec(xa, pciMaster, pciDetails, pcuMaster, pcuDetails
                , models, models.flatMap(_.lines).map(FinancialsTransactionDetails.encodeIt4)
                , oldmodels.map(FinancialsTransaction.encodeIt2)
                , oldmodels.flatMap(_.lines).map(FinancialsTransactionDetails.encodeIt2))

  def transact(s: Session[Task], models: List[FinancialsTransaction], newLines2Insert: List[FinancialsTransactionDetails]
               , oldmodels: List[FinancialsTransaction], oldLines2Update: List[FinancialsTransactionDetails]
               , oldLines2Delete: List[FinancialsTransactionDetails]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciMaster =>
        s.prepareR(UPDATE).use: pcuMaster =>
          s.prepareR(insertDetails).use: pciDetails =>
            s.prepareR(UPDATE_DETAILS).use: pcuDetails =>
              s.prepareR(DELETE_DETAILS).use: pcdDetails =>
                tryExec(xa, pciMaster, pciDetails, pcuMaster, pcuDetails, pcdDetails
                  , models, newLines2Insert.map(FinancialsTransactionDetails.encodeIt4)
                  , oldmodels.map(FinancialsTransaction.encodeIt2), oldLines2Update.map(FinancialsTransactionDetails.encodeIt2)
                  , oldLines2Delete.map(FinancialsTransactionDetails.encodeIt3))
                
  override def create(c: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = create(List(c))

  override def create(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, buildId(models)))
        .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)

  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = modify(List(model))

  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] ={
    val oldLines2Update = models.flatMap(_.lines).filter(line => line.id > 0 && line.company.contains("-"))
      .map(line => line.copy(company = line.company.replace("-", "")))
    val newLine2Insert = models.flatMap(_.lines).filter(line => line.id === -1L && line.company.contains("-"))
      .map(line => line.copy(company = line.company.replace("-", "")))

    val oldLine2Delete = models.flatMap(_.lines).filter(line => line.transid === -2L)
    //  ZIO.logInfo(s"models ${models}") *>
    //    ZIO.logInfo(s"oldLines2Update ${oldLines2Update}") *>
    //    ZIO.logInfo(s"newLine2Insert ${newLine2Insert}")*>
    //    ZIO.logInfo(s"oldLine2Delete ${oldLine2Delete}")*>
    postgres
      .use:
        session =>
          transact(session, List.empty, newLine2Insert, models, oldLines2Update, oldLine2Delete)
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)
}
  def list(p: (Int, String)):ZIO[Any, RepositoryError, List[FinancialsTransaction]] = queryWithTx(postgres, p, ALL)

  private def  getDetails(p:(Long, String)): ZIO[Any, RepositoryError, List[FinancialsTransactionDetails]] = for {
    details <- queryWithTx(postgres, p, DETAILS1)
  }yield details

  private def getByTransId1(trans: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    lines_ <- getDetails(trans.id1, trans.company)
  } yield trans.copy(lines = if (lines_.nonEmpty) lines_ else List.empty[FinancialsTransactionDetails])

  private def withLines(trans: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] =
    getByTransId1(trans)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- list(p)
    details     <-  transactions.map(withLines).flip
  } yield details

  override def getById(p: (Long, Int, String)):ZIO[Any, RepositoryError, FinancialsTransaction] = for { 
    transaction <- queryWithTxUnique(postgres, p, BY_ID)
    details <- withLines(transaction)
} yield details

  override def getById1(p: (Long, Int, String)): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    transaction <- queryWithTxUnique(postgres, p, BY_ID1)
    details <- withLines(transaction)
  } yield details
  
  override def getBy(ids: List[Long],  modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    details <- transactions.map(withLines).flip
  } yield details
    
  
  override def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =for {
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


  val mfEncoder: Encoder[FinancialsTransaction] = financialsTransactionCodec4.values.contramap(FinancialsTransaction.encodeIt4)

  def detailsDecoder: Decoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.map:
      case (id, transid, account, side, oaccount, amount, duedate, text, currency, company, accountName, oaccountName) =>
        FinancialsTransactionDetails(id, transid, account, side, oaccount, amount.bigDecimal, toInstant(duedate), text
        , currency,  company, accountName, oaccountName)

  def base =
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
           WHERE id  (IN ${int8.list(nr)}) AND modelid = $int4 AND company = $varchar
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

  val BY_ID1: Query[Long *: Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id1 = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE  modelid = $int4 AND company = $varchar
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
           WHERE  transid  in ${int8.list(n)}  AND company = $varchar
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
         (oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content) VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[FinancialsTransaction.TYPE]] =
    sql"""INSERT INTO master_compta 
          (oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content)
         VALUES ${financialsTransactionCodec.values.list(n)}""".command

  val insertDetails: Command[FinancialsTransactionDetails.D_TYPE4] =
    sql"""INSERT INTO details_compta (transid, account, side, oaccount, amount, duedate, text, currency, company
          , account_name, oaccount_name) VALUES  ($financialsDetailsTransactionCodec4)""".command
    
  def insertAllDetails(n:Int): Command[List[FinancialsTransactionDetails.D_TYPE4]] =
    sql"""INSERT INTO details_compta (transid, account, side, oaccount, amount, duedate, text, currency, company
          , account_name, oaccount_name) VALUES (${financialsDetailsTransactionCodec4.values.list(n)})""".command

  val UPDATE: Command[FinancialsTransaction.TYPE2] =
    sql"""UPDATE master_compta
          SET oid = $int8, costcenter = $varchar, account = $varchar, text=$varchar, type_journal = $int4, file_content = $int4
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
  