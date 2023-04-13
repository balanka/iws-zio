package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionx}
import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

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
      .from(transaction)

  private val SELECT_LINE = select(lid_, transid, laccount_, side_, oaccount_, amount_, duedate_, ltext_, currency_).from(transactionDetails)
  private val CURRVAL     = FunctionDef[String, Long](FunctionName("currval"))
  //private val LASTVAL     = FunctionDef[String, Long](FunctionName("lastval"))

  private def getLastTransid: ZIO[Any, RepositoryError, Option[Long]] = {
    val selectAll = select(CURRVAL("master_compta_id_seq"))
    //val selectAll = select(LASTVAL("master_compta_id_seq"))

    execute(selectAll).runHead.provideAndLog(driverLayer)

  }


  private def buildDetailsQuery(models: List[FinancialsTransactionDetails]) = {
    if (models.isEmpty) ZIO.succeed(0) else
    for {
      nextTransId <- getLastTransid
      _ <- ZIO.logInfo(s"Next transaction id is ${nextTransId} ${models}")
       lines = models.map(l => l.copy(transid = nextTransId.getOrElse(-1L)))
       query = insertInto(transactionDetailsInsert)(transidx, laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx).values(lines.map(toTuple))
      _        <- ZIO.logInfo(s"Query to insert FinancialsTransactionDetails  ${renderInsert(query)}")
      inserted <- execute(query).provideAndLog(driverLayer)
    } yield inserted
  }


  private def buildQuery(model: FinancialsTransaction)=
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


  private def runInsert(model: FinancialsTransaction) = {
    val query = buildQuery(model)
    val result = for {
      _ <- ZIO.logInfo(s"Query to insert Transaction is ${renderInsert(query)}")
      transactionInserted <- execute(query).provideAndLog(driverLayer)
      insertedDetails <- buildDetailsQuery(model.lines)
    } yield (transactionInserted + insertedDetails)
    result
  }
  private def insert(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    models.map(runInsert).flip.map(_.sum)

  override def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]     = insert(List(model))
  override def create(model: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]        = insert(model)

  private def buildDeleteDetails(id_ :Long):Delete[com.kabasoft.iws.domain.FinancialsTransactionDetails]  =
    deleteFrom(transactionDetails).where(lid_ === id_)


  override def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val deleteQuery = deleteFrom(transaction).where((id_ === id) && (company_ === companyId))
    ZIO.logInfo(s"Delete  FinancialsTransactionDetails  ${renderDelete(deleteQuery)}")*>
    execute(deleteQuery)
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))
  }

  private def buildUpdateDetails(
    details: FinancialsTransactionDetails
  ): Update[FinancialsTransactionDetails]=
    update(transactionDetails)
      .set(transid, details.transid)
      .set(laccount_, details.account)
      .set(side_, details.side)
      .set(amount_, details.amount)
      .set(duedate_, details.duedate)
      .set(ltext_, details.text)
      .set(currency_, details.currency)
      .where(lid_ === details.id)

  private def build(trans: FinancialsTransaction):Update[FinancialsTransactionx]  =
    update(transaction)
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

  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = for{
  nr <-  ZIO.foreach(models)(modify).map(_.sum)
  } yield nr
  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    def insertPredicate(line: FinancialsTransactionDetails) = line.id <= -1L
    def deletePredicate(line: FinancialsTransactionDetails) = line.transid == -2L

    val splitted = model.lines.partition(insertPredicate)
    val splitted2 = splitted._2.partition(deletePredicate)
    val newLines = splitted._1
    val deletedLineIds = splitted2._1.map(line => line.id)
    val oldLines = splitted._2
    val detailsUpdate = oldLines.map(d => buildUpdateDetails(d))
    val update_       = build(model)
    val result: ZIO[Any, RepositoryError, Int] = for {
      insertedDetails <- buildDetailsQuery( newLines)
      deletedDetails <-  executeBatchDelete(deletedLineIds.map(buildDeleteDetails))
      .provideLayer(driverLayer).mapBoth(e => RepositoryError(e.getMessage), _.sum)
      updatedDetails <- executeBatchUpdate(detailsUpdate)
        .provideLayer(driverLayer).mapBoth(e => RepositoryError(e.getMessage), _.sum)
      updatedTransactions <- execute(update_)
              .provideLayer(driverLayer)
              .mapError(e => RepositoryError(e.getMessage))
    } yield updatedTransactions + insertedDetails+updatedDetails+deletedDetails
    result
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


  private[this] def find4Period_(
    fromPeriod: Int,
    toPeriod: Int,
    companyId: String
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
    ZIO.logInfo(s"Query to execute getById ${id} is ${renderRead(selectAll)}") *>
     // execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
      execute(selectAll.to(x => {println(x); FinancialsTransaction.apply(x)}))
        .findFirstLong(driverLayer, id)
  }

  override def getByModelId(modelId:(Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =for {
    trans <- getByModelIdX(modelId._1,modelId._2).mapZIO(tr => getTransWithLines(tr.id, modelId._2)).runCollect.map(_.toList)
  }yield trans

  override def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((modelid_ === modelId) && (company_ === companyId))
    ZStream.fromZIO(ZIO.logInfo(s"Query to execute getByModelIdX modelid:  ${modelId}  companyId:  ${companyId}is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
  }

}

object TransactionRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryImpl(_))
}
