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
      id1_,
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

  private val SELECT_LINE = select(lid_, transid, laccount_, side_, oaccount_, amount_, duedate_, ltext_, currency_, accountName_, oaccountName_).from(transactionDetails)


  @nowarn
  def create2(transactions: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    transact(create2s(transactions))
      .mapError(e => RepositoryError(e.toString))
      .provideLayer(driverLayer)

//  def create2_(trans : FinancialsTransaction): ZIO[SqlTransaction, Exception, Int] = {
//    val transid1 = newCreate()
//     val model = trans.copy(id1 = transid1, lines = trans.lines.map(_.copy(transid = transid1)))
//    val insertNewLines_ = buildInsertNewLines(model.lines)
//    for {
//      x <- buildInsertQuery(model).run
//      y <- insertNewLines_.run
//      _ <- ZIO.logDebug(s"Create transaction stmt       ${renderInsert(buildInsertQuery(model))} ") *>
//        ZIO.logInfo(s"Create line transaction stmt   ${renderInsert(insertNewLines_)} ")
//    } yield x + y
//  }

  @nowarn
   def create2(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] =
    transact(create2s(List(model)))
          .tap(tr => ZIO.logInfo(s"Create transaction result ${tr} "))
          .mapError(e => RepositoryError(e.toString))
          .provideLayer(driverLayer)


  @nowarn
  override def create(ftr : FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] =
    if (ftr.id > 0) {
      update(ftr)
    } else {
      create2(ftr) *> getByTransId1(ftr.id1, ftr.company)
    }


  @nowarn
  override def create(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = {
    create2(models) *>
      getByTransId1x(models.map(m => m.id), models.head.company)
  }

  private def buildDeleteDetails(ids: List[Long]): Delete[FinancialsTransactionDetails] =
    deleteFrom(transactionDetails).where(lid_ in ids)

  override def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val deleteQuery = deleteFrom(transactions).where((id_ === id) && (company_ === companyId))
    ZIO.logDebug(s"Delete  FinancialsTransactionDetails  ${renderDelete(deleteQuery)}")*>
      execute(deleteQuery)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  private def buildUpdateDetails(details: FinancialsTransactionDetails): Update[FinancialsTransactionDetails] =
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

  private def build(trans: FinancialsTransaction):Update[FinancialsTransactionx]  = {
    val transid1= if(trans.id>0) trans.id else trans.id1
    update(transactions)
      .set(oid_, trans.oid)
      .set(id1_, transid1)
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
  }

  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    models.map(modify).flip.map(_.sum)

  @nowarn
  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    if (model.id <= 0) {
      create2(model)
    } else {
      val newLines = model.lines.filter(_.id == -1L).map(l=>l.copy(transid = model.id))
      val deletedLineIds = model.lines.filter(_.transid == -2L).map(line => line.id)
      val oldLines2Update = model.lines.filter(_.id > 0L).map(l=>l.copy(transid = model.id))
      val update_ = build(model)

      val result = for {
        insertedDetails <- ZIO.when(newLines.nonEmpty)(buildInsertNewLines(newLines).run)
        deletedDetails <- ZIO.when(deletedLineIds.nonEmpty)(buildDeleteDetails(deletedLineIds).run)
        updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(d => buildUpdateDetails(d).run).flip.map(_.sum))
        updatedTransactions <- update_.run
      } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions

      def buildResult = transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)

      ZIO.logInfo(s"New lines transaction insert stmt ${renderInsert(buildInsertNewLines(newLines))}") *>
        ZIO.logInfo(s"Update lines transaction update stmt ${oldLines2Update.map(buildUpdateDetails).map(renderUpdate)}") *>
        ZIO.logInfo(s"Delete lines transaction  stmt ${renderDelete(buildDeleteDetails(deletedLineIds))}") *>
        ZIO.logInfo(s"Modify transaction stmt ${renderUpdate(update_)}") *> buildResult
    }
  }

  @nowarn
  override def update(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] =
    if (model.id<=0){
      create(model)
    }else {
    modify(model) *>getByTransId(model.id, model.company)
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
    trans <- list1(companyId).mapZIO(tr => withLines(tr)).runCollect.map(_.toList)
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
    val selectAll = SELECT_LINE.where(transid === trans.id1)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getLineByTransId1 is ${renderRead(selectAll)}")) *>
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

   private def getByTransId1x(id: (List[Long], String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- getByIds(id._1, id._2)
    trans <-transactions.map(withLines).flip
   } yield trans

   private def withLines(trans: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] =
    getByTransId1((trans.id1, trans.company))


  override def getByTransId1(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans <- getById1(id._1, id._2)
    lines_ <- getLineByTransId(trans).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)
  def getById(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id_ === id))
    ZIO.logDebug(s"Query to execute getById ${id} is ${renderRead(selectAll)}") *>
      execute(selectAll.to(x => FinancialsTransaction.apply(x)))
        .findFirstLong(driverLayer, id)
  }

  def getByIds(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id_ in  ids))
   // ZStream.fromZIO(ZIO.logDebug(s"Query to execute getById ${ids} is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
        .runCollect.map(_.toList)
  }

  def getById1(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id1_ === id))
    ZIO.logInfo(s"Query to execute getById1 ${id} is ${renderRead(selectAll)}") *>
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