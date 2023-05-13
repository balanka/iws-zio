package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionx}
import zio.{ZIO, _}
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._
import scala.annotation.nowarn

final class TransactionRepositoryImpl(pool: ConnectionPool) extends TransactionRepository with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val SELECT2     =
    select(
      id_,
      oid_,
      account_,
      costcenter_,
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

  private val SELECT_LINE = select(lid_, transid, laccount_, side_, oaccount_, amount_, duedate_, ltext_, currency_).from(transactionDetails)
  //private val CURRVAL     = FunctionDef[String, Long](FunctionName("currval"))
  //private val LASTVAL     = FunctionDef[String, Long](FunctionName("lastval"))

//  private def getLastTransid: ZIO[Any, RepositoryError, Long] =
//    execute(selectAll).findFirstLong(driverLayer, -4711L)

  private def insertNewLines(models: List[FinancialsTransactionDetails], parentId:Long)= {
    val lines = models.map(l => l.copy(transid = parentId))
    val query = insertInto(transactionDetailsInsert)(transidx, laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx).values(lines.map(toTuple))
    query
  }

  private def buildInsertQuery(model: FinancialsTransaction)=
    insertInto(transactionInsert)(
      oidx,
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
  override def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    val trans = for {
      _<-ZIO.logInfo(s"Result createtransaaction  ${renderInsert(buildInsertQuery(model))} ")
      x <- buildInsertQuery(model).run

    //  y <- insertNewLines(model.lines, -1L).run
    } yield x//+y
    val r = transact(trans)
        .tap(tr => ZIO.logInfo(s"Result createtransaaction  ${tr} "))
        .mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
    r
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

  private def buildUpdateDetails(details: FinancialsTransactionDetails): ZIO[SqlTransaction, Exception, Int] = {
    val updateSQL= update(transactionDetails)
      .set(transid, details.transid)
      .set(laccount_, details.account)
      .set(side_, details.side)
      .set(amount_, details.amount)
      .set(duedate_, details.duedate)
      .set(ltext_, details.text)
      .set(currency_, details.currency)
      .where(lid_ === details.id)//.run
    ZIO.logInfo(s"Delete  FinancialsTransactionDetails  ${renderUpdate(updateSQL)}")*>
    updateSQL.run
  }

  private def build(trans: FinancialsTransaction):Update[FinancialsTransactionx]  =
    update(transactions)
      .set(oid_, trans.oid)
      .set(costcenter_, trans.costcenter)
      .set(account_, trans.account)
      .set(transdate_, trans.transdate)
      .set(modelid_, trans.modelid)
      .set(company_, trans.company)
      .set(text_, trans.text)
      .set(type_journal_, trans.typeJournal)
      .set(file_content_, trans.file_content)
      .where((id_ === trans.id) && (company_ === trans.company))

  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = for {
    nr <-  ZIO.foreach(models)(modify).map(_.sum)
  } yield nr

  @nowarn
  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    def insertPredicate(line: FinancialsTransactionDetails) = line.id <= -1L
    def deletePredicate(line: FinancialsTransactionDetails) = line.transid == -2L

    val splitted = model.lines.partition(insertPredicate)
    val splitted2 = splitted._2.partition(deletePredicate)
    val newLines = splitted._1
    val deletedLineIds = splitted2._1.map(line => line.id)
    val oldLines = splitted._2
    val oldLines2Update = oldLines.filter(_.transid==model.id)
    val update_       = build(model)

    val result = for {
      insertedDetails <- ZIO.when(newLines.size > 0)(insertNewLines(newLines, model.id).run)
      deletedDetails <- ZIO.when(deletedLineIds.size > 0)(buildDeleteDetails(deletedLineIds).run)
      updatedDetails <- ZIO.when(oldLines2Update.size > 0)(oldLines2Update.map(buildUpdateDetails).flip.map(_.sum))
      updatedTransactions <- update_.run
    } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions

    def buildResult =  transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)

  ZIO.logInfo(s"SQL to execute modify is ${renderUpdate(update_)}")*>buildResult

  }

  private[this] def list1(companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where(company_ === companyId)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
  }
  override def all(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    trans <- list1(companyId).mapZIO(tr => getTransWithLines(tr.id, companyId)).runCollect.map(_.toList)
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
      .mapZIO(tr => getTransWithLines(tr.id, tr.company))
  } yield trans

  private[this] def getLineByTransId(id: Long): ZStream[Any, RepositoryError, FinancialsTransactionDetails] = {
    val selectAll = SELECT_LINE.where(transid === id)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getLineByTransId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(x => FinancialsTransactionDetails.apply(x)))
        .provideDriver(driverLayer)
  }

  private[this] def getTransWithLines(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans  <- getByTransId_(id, companyId)
    lines_ <- getLineByTransId(id).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)

  private[this] def getByTransId_(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans <- getById(id, companyId)
  } yield trans

  override def getByTransId(id:(Long,  String)): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans  <- getById(id._1, id._2)
    lines_ <- getLineByTransId(id._1).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)
  def getById(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id_ === id))
    ZIO.logDebug(s"Query to execute getById ${id} is ${renderRead(selectAll)}") *>
      execute(selectAll.to(x => FinancialsTransaction.apply(x)))
        .findFirstLong(driverLayer, id)
  }

  override def getByModelId(modelId:(Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    trans <- getByModelIdX(modelId._1,modelId._2).mapZIO(tr => getTransWithLines(tr.id, modelId._2)).runCollect.map(_.toList)
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