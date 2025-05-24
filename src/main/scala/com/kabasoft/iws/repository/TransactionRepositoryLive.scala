package com.kabasoft.iws.repository

import cats._
import cats.effect.Resource
import cats.syntax.all._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.prelude.FlipOps
import zio.interop.catz._
import zio.{ZIO, _}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Article, Transaction, TransactionDetails, common}

import java.time.{Instant, LocalDateTime, ZoneId}

final case  class TransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                            , accRepo: AccountRepository
                                            , articleRepo: ArticleRepository) extends TransactionRepository, MasterfileCRUD:

  import TransactionRepositorySQL._

  def buildId(transaction: Transaction): Transaction = {
    if(transaction.id1 >0L) transaction else
    {
      List(transaction).zipWithIndex.map { case (ftr, i) =>
        val idx = Instant.now().getNano + i.toLong
        ftr.copy(id1 = idx, lines = ftr.lines.map(_.copy(transid = idx)), period = common.getPeriod(ftr.transdate))
      }.headOption.getOrElse(transaction)
    }
  }

  def transact(s: Session[Task], models: List[Transaction]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(insert).use: pciMaster =>
        s.prepareR(insertDetails).use: pciDetails =>
          tryExec(xa, pciMaster, pciDetails, models, models.flatMap(_.lines).map(TransactionDetails.encodeIt4))
  
  def transact(s: Session[Task], models: List[Transaction], newLines2Insert: List[TransactionDetails]
             , oldmodels: List[Transaction], oldLines2Update: List[TransactionDetails]
             ,  oldLines2Delete: List[TransactionDetails]): Task[Unit] =
     s.transaction.use: xa =>
       s.prepareR(insert).use: pciMaster =>
         s.prepareR(UPDATE).use: pcuMaster =>
           s.prepareR(insertDetails).use: pciDetails =>
             s.prepareR(UPDATE_DETAILS).use: pcuDetails =>
               s.prepareR(DELETE_DETAILS).use: pcdDetails =>
                 tryExec(xa, pciMaster, pciDetails, pcuMaster, pcuDetails, pcdDetails
                  , models, newLines2Insert.map(TransactionDetails.encodeIt4)
                  , oldmodels.map(Transaction.encodeIt2), oldLines2Update.map(TransactionDetails.encodeIt2)
                  , oldLines2Delete.map(TransactionDetails.encodeIt3))

  def transact(s: Session[Task], models: List[Transaction], newLines2Insert: List[TransactionDetails]
             , oldmodels: List[Transaction], oldLines2Update: List[TransactionDetails]
             , oldmodels2Delete: List[Transaction], oldLines2Delete: List[TransactionDetails]): Task[Unit] =
  s.transaction.use: xa =>
    s.prepareR(insert).use: pciMaster =>
      s.prepareR(UPDATE).use: pcuMaster =>
        s.prepareR(insertDetails).use: pciDetails =>
          s.prepareR(UPDATE_DETAILS).use: pcuDetails =>
            s.prepareR(DELETE).use: pcdMaster =>
              s.prepareR(DELETE_DETAILS).use: pcdDetails =>
                tryExec(xa, pciMaster, pciDetails, pcuMaster, pcuDetails, pcdMaster, pcdDetails
                  , models, newLines2Insert.map(TransactionDetails.encodeIt4)
                  , oldmodels.map(Transaction.encodeIt2), oldLines2Update.map(TransactionDetails.encodeIt2)
                  , oldmodels2Delete.map(Transaction.encodeIt3)
                  , oldLines2Delete.map(TransactionDetails.encodeIt3))
  override def create(c: Transaction): ZIO[Any, RepositoryError, Int] = create(List(c))

  override def create(models: List[Transaction]): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          transact(session, models.map(buildId).map(tr=>
            tr.copy(lines = tr.lines.map(line=>line.copy(company = line.company.replace("-", "")))))))
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)

  override def modify(model: Transaction): ZIO[Any, RepositoryError, Int] = modify(List(model))

  override def modify(models: List[Transaction]): ZIO[Any, RepositoryError, Int] = {
    val oldLines2Update = models.flatMap(_.lines).filter(line => line.id > 0 && line.company.contains("-"))
      .map(line => line.copy(company = line.company.replace("-", "")))
    val newLine2Insert = models.flatMap(_.lines).filter(line => line.id === -1L && line.company.contains("-"))
      .map(line => line.copy(company = line.company.replace("-", "")))

    val oldLine2Delete = models.flatMap(_.lines).filter(line => line.transid === -2L) 
//    ZIO.logInfo(s"models ${models}") *>
//      ZIO.logInfo(s"oldLines2Update ${oldLines2Update}") *>
//      ZIO.logInfo(s"newLine2Insert ${newLine2Insert}") *>
//      ZIO.logInfo(s"oldLine2Delete ${oldLine2Delete}") *>
      postgres
        .use:
          session =>
            transact(session, List.empty, newLine2Insert, models, oldLines2Update, oldLine2Delete)
        .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size)
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
    _ <-ZIO.logInfo(s"lines_.map(_.article) ${lines_.map(_.article)}")
    articles <- articleRepo.getBy(lines_.map(_.article), Article.MODELID, trans.company)
   // vats <- vatRepo.getBy(trans.lines.map(_.vatCode), Vat.MODEL_ID, trans.company)
  } yield trans.copy(lines = if (lines_.nonEmpty) {
    lines_.map(line =>line.copy(articleName=articles.find(art=>art.id ===line.article).fold(line.articleName)(article =>article.name),
                                unit=articles.find(art=>art.id ===line.article).fold(line.unit)(article =>article.quantityUnit)))
                           // vatName=vats.find(vat=>vat.id ===line.vatCode).fold(line.vatName)(vat =>vat.name) ))
  } else List.empty[TransactionDetails])

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
           WHERE id  IN (${int8.list(nr)}) AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
           
  val BY_ID1: Query[Long *: Int *: String *: EmptyTuple, Transaction] =
    sql"""SELECT id, oid, id1, store, account, transdate, enterdate, postingdate, period, posted, modelid, company, text
           FROM   transaction
           WHERE id1 = $int8 AND modelid = $int4 AND company = $varchar
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
  
  val insert: Command[Transaction] =
    sql"""INSERT INTO transaction (oid, id1, store, account, enterdate, transdate, postingdate, period, posted, modelid
         , company, text) VALUES $mfEncoder""".command

  def insertAll(n:Int): Command[List[Transaction.TYPE]] = 
    sql"""INSERT INTO transaction (oid, id1, store, account, enterdate, transdate, postingdate, period, posted, modelid
         , company, text ) VALUES ${transactionCodec1.values.list(n)}""".command

  val insertDetails: Command[TransactionDetails.D_TYPE1] =
    sql"""INSERT INTO transaction_details (transid, article, article_name, quantity, unit, price, currency, duedate, vat_code
          , text, company) VALUES ($transactionDetailsCodec2 )""".command
    
//  def insertAllDetails(n:Int): Command[List[TransactionDetails.D_TYPE1]] =
//    sql"""INSERT INTO transaction_details (transid, article, article_name, quantity, unit, price, currency
//          , duedate, vat_code, text, company) VALUES ${transactionDetailsCodec.values.list(n)}""".stripMargin.command

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
          , duedate = $timestamp, text=$varchar, article_name = $varchar, vat_code = $varchar
          WHERE id=$int8 and company= $varchar""".command

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM transaction WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command

  def DELETE_All: Command[Void] = sql"DELETE FROM transaction WHERE  company = '-1000'".command
  val DELETE_DETAILS : Command[(Long, String)] = sql"DELETE FROM transaction_details WHERE id = $int8 AND company = $varchar".command
  val DELETE_ALL_DETAILS: Command[Void] = sql"DELETE FROM transaction_details WHERE  id=-2 and company = '-1000'".command
  val NEXT_ID:Query[Void, Long] = sql"SELECT NEXTVAL('master_compta_id_seq')".query(int8)
  //id  IN (${varchar.list(nr)})