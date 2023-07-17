package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionx}
import zio.{ZIO, _}
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

import scala.annotation.nowarn
import scala.util.Random

final class TransactionRepositoryImpl(pool: ConnectionPool) extends TransactionRepository with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val SELECT2     =
    select(
      id_,
      oid_,
      id2_,
      costcenter_,
      account_,
      transdate_,
      enterdate_,
      postingdate_,
      period_,
      posted_,
      modelid_,
      company_,
      text_,
      type_journal_,
      file_content_
    )
      .from(transactions)

  private val SELECT_LINE = select(lid_, transid, transid2, laccount_, side_, oaccount_, amount_, duedate_, ltext_, currency_).from(transactionDetails)
  //private val CURRVAL     = FunctionDef[String, Long](FunctionName("currval"))
  //private val LASTVAL     = FunctionDef[String, Long](FunctionName("lastval"))

//  private def getCurrentTransid: ZIO[Any, RepositoryError, Option[Long]] = {
//    val selectAll = select(CURRVAL("master_compta_id_seq"))
//    execute(selectAll).runHead.provideAndLog(driverLayer)
//  }

  private def insertNewLines(models: List[FinancialsTransactionDetails])=
   insertInto(transactionDetailsInsert)(transidx, transid2x, laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx).values(models.map(toTuple))


  private def buildInsertQuery(model: FinancialsTransaction)=
    insertInto(transactionInsert)(
      oidx,
      id2,
      costcenterx,
      accountx,
      transdatex,
      enterdatex,
      postingdatex,
      periodx,
      postedx,
      modelidx,
      companyx,
      textx,
      type_journalx,
      file_contentx
    ).values( toTupleC(model))

  @nowarn
  override def create(model_ : FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    //val transId = if(model_.id>0){
     if(model_.id>0){
      model_.id
      modify(model_)
    }else{
      //model_.enterdate.getEpochSecond+model_.enterdate.getNano
      val transid2x = new String(Random.alphanumeric.take(10).toArray)
      val model = model_.copy(id2 = transid2x, lines = model_.lines.map(_.copy( transid2 = transid2x)))
     // val model = model_.copy(id2 = transid2x, lines = model_.lines.map(_.copy(transid = transId, transid2 = transid2x)))
      val trans = for {
        _ <- ZIO.logInfo(s"Create transaction stmt   ${renderInsert(buildInsertQuery(model))} ")
        x <- buildInsertQuery(model).run
        y <- insertNewLines(model.lines).run
        _ <- ZIO.logInfo(s"Create line transaction stmt   ${renderInsert(insertNewLines(model.lines))} ")
      } yield x + y
      val r = transact(trans)
        .tap(tr => ZIO.logInfo(s"Create transaction result ${tr} "))
        .mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
      r
    }

  }
  override def create(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]        = models.map(create).flip.map(_.sum)
  private def buildDeleteDetails(ids: List[Long]): Delete[FinancialsTransactionDetails] =
    deleteFrom(transactionDetails).where(lid_ in ids)

  override def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val deleteQuery = deleteFrom(transactions).where((id_ === id) && (company_ === companyId))
    ZIO.logDebug(s"Delete  FinancialsTransactionDetails  ${renderDelete(deleteQuery)}")*>
      execute(deleteQuery)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  private def buildUpdateDetails_(details: FinancialsTransactionDetails): Update[FinancialsTransactionDetails] =
    update(transactionDetails)
      .set(transid, details.transid)
      .set(laccount_, details.account)
      .set(oaccount_, details.oaccount)
      .set(side_, details.side)
      .set(amount_, details.amount)
      .set(duedate_, details.duedate)
      .set(ltext_, details.text)
      .set(currency_, details.currency)
      .where(lid_ === details.id)

//  private def buildUpdateDetails(details: FinancialsTransactionDetails): ZIO[SqlTransaction, Exception, Int] = {
//    val updateSQL= buildUpdateDetails_(details)
//    ZIO.logInfo(s"Update  FinancialsTransactionDetails  ${renderUpdate(updateSQL)}")*>
//    updateSQL.run
//  }

  private def build(trans: FinancialsTransaction):Update[FinancialsTransactionx]  =
    update(transactions)
      .set(oid_, trans.oid)
      .set(costcenter_, trans.costcenter)
      .set(account_, trans.account)
      .set(transdate_, trans.transdate)
      .set(modelid_, trans.modelid)
      .set(company_, trans.company)
      .set(text_, trans.text)
      .set(period_, trans.period)
      .set(posted_, trans.posted)
      .set(type_journal_, trans.typeJournal)
      .set(file_content_, trans.file_content)
      .where((id_ === trans.id) && (company_ === trans.company))

  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = for {
    nr <-  ZIO.foreach(models)(modify).map(_.sum)
  } yield nr

  @nowarn
  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    if (model.id<=0){
      create(model)
    }else {
      val newLines = model.lines.filter(_.id == -1L)
      val deletedLineIds = model.lines.filter(_.transid == -2L).map(line => line.id)
      val oldLines2Update = model.lines.filter(l => l.transid == model.id && (l.id > 0L))
      val update_ = build(model)

      val result = for {
        insertedDetails <- ZIO.when(newLines.nonEmpty)(insertNewLines(newLines).run)
        deletedDetails <- ZIO.when(deletedLineIds.nonEmpty)(buildDeleteDetails(deletedLineIds).run)
        updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(d => buildUpdateDetails_(d).run).flip.map(_.sum))
        //updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(d=>buildUpdateDetails(d).run).flip.map(_.sum))
        //updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(buildUpdateDetails).flip)
        updatedTransactions <- update_.run
      } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions

      def buildResult = transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)

      ZIO.logInfo(s"New lines transaction insert stmt ${renderInsert(insertNewLines(newLines))}") *>
        ZIO.logInfo(s"Update lines transaction update stmt ${oldLines2Update.map(buildUpdateDetails_).map(renderUpdate)}") *>
        ZIO.logInfo(s"Delete lines transaction  stmt ${renderDelete(buildDeleteDetails(deletedLineIds))}") *>
        ZIO.logInfo(s"Modify transaction stmt ${renderUpdate(update_)}") *> buildResult
    }
  }

  override def updatePostedField(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = for {
    nr <- ZIO.foreach(models)(updatePostedField).map(_.sum)
  } yield nr

  @nowarn
  override def updatePostedField(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    val updateSQL = update(transactions).set(posted_, true).where((id_ === model.id) && (company_ === model.company))
    val result = for {
      _<- ZIO.logDebug(s"Query to execute findAll is ${renderUpdate(updateSQL)}")
      m <- updateSQL.run
    } yield m
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }
  private[this] def list1(companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where(company_ === companyId)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
  }
  override def all(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    trans <- list1(companyId).mapZIO(tr => getTransWithLines(tr, companyId)).runCollect.map(_.toList)
  } yield trans

  private[this] def find4Period_( fromPeriod: Int, toPeriod: Int, companyId: String
                                ): ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2
      .where((company_ === companyId) && (period_ >= fromPeriod) && (period_ <= toPeriod))
      .orderBy(account_.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period1 is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
  }
  def find4Period(
                   fromPeriod: Int,
                   toPeriod: Int,
                   companyId: String
                 ): ZStream[Any, RepositoryError, FinancialsTransaction] = for {
    trans <- find4Period_(fromPeriod, toPeriod, companyId)
      .mapZIO(tr => getTransWithLines(tr, tr.company))
  } yield trans

  private[this] def getLineByTransId(trans: FinancialsTransaction): ZStream[Any, RepositoryError, FinancialsTransactionDetails] = {
    //val selectAll = SELECT_LINE.where(transid === trans.id || transid2 === trans.id2)
    val selectAll = SELECT_LINE.where(transid === trans.id)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getLineByTransId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(x => FinancialsTransactionDetails.apply(x)))
        .provideDriver(driverLayer)
  }

  private[this] def getTransWithLines(trans: FinancialsTransaction, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
      trans  <- getByTransId_(trans.id, companyId)
      lines_ <- getLineByTransId(trans).runCollect.map(_.toList)
    } yield trans.copy(lines = lines_)//.filter(_.transid==trans.id))

  private[this] def getByTransId_(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans <- getById(id, companyId)
  } yield trans

  override def getByTransId(id:(Long,  String)): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans  <- getById(id._1, id._2)
    lines_ <- getLineByTransId(trans).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)
  def getById(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id_ === id))
    ZIO.logDebug(s"Query to execute getById ${id} is ${renderRead(selectAll)}") *>
      execute(selectAll.to(x => FinancialsTransaction.apply(x)))
        .findFirstLong(driverLayer, id)
  }

  override def getByModelId(modelId:(Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    trans <- getByModelIdX(modelId._1,modelId._2).mapZIO(tr => getTransWithLines(tr, modelId._2)).runCollect.map(_.toList)
  }yield trans

  override def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((modelid_ === modelId) && (company_ === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelIdX modelid:  ${modelId}  companyId:  ${companyId}is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
  }

}

object TransactionRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryImpl(_))
}