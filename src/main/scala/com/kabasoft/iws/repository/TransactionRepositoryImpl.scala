package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._
import zio.{ZIO, _}

import scala.annotation.nowarn

final class TransactionRepositoryImpl(pool: ConnectionPool) extends TransactionRepository with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val SELECT2     =
    select(
      id_,
      oid_,
      id1_,
      store_,
      costcenter_,
      transdate_,
      enterdate_,
      postingdate_,
      period_,
      posted_,
      modelid_,
      company_,
      text_
    )
      .from(transactions)

  private val SELECT_LINE = select(lid_, transid, article_, article_name_, quantity_, unit_, price_, currency_, duedate_, ltext_ ).from(transactionDetails)

  @nowarn
  override def create2(transactions: List[Transaction]): ZIO[Any, RepositoryError, Int] =
    transact(create2s( buildId1(transactions)))
      .mapError(e => { println(e.getMessage)
        RepositoryError(e.toString)})
      .provideLayer(driverLayer)
  @nowarn
  override def create(ftr : Transaction): ZIO[Any, RepositoryError, Transaction] =
    if (ftr.id > 0) {
      update(ftr)
    } else  for {
        nr <-  create2(List(ftr)) *> getByTransId1(ftr.id1, ftr.company)
      } yield nr

  @nowarn
  override def create(models: List[Transaction]): ZIO[Any, RepositoryError, List[Transaction]] = for{
     nr <-  create2(models) *> getByTransId1x(models.map(m => m.id), models.head.company)
    } yield nr

  private def buildDeleteDetails(ids: List[Long]): Delete[TransactionDetails] =
    deleteFrom(transactionDetails).where(lid_ in ids)
  override def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val deleteQuery = deleteFrom(transactions).where((id_ === id) && (company_ === companyId))
    ZIO.logDebug(s"Delete  FinancialsTransactionDetails  ${renderDelete(deleteQuery)}")*>
      execute(deleteQuery)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  private def buildUpdateDetails(details: TransactionDetails): Update[TransactionDetails] = {
    update(transactionDetails)
      .set(transid, details.transid)
      .set(article_, details.article)
      .set(article_name_, details.articleName)
      .set(quantity_, details.quantity)
      .set(unit_, details.unit)
      .set(price_, details.price)
      .set(currency_, details.currency)
      .set(duedate_, details.duedate)
      .set(ltext_, details.text)
      .where(lid_ === details.id)
  }

  private def build(trans: Transaction):Update[Transactionx]  = {
    val transid1= if(trans.id>0) trans.id else trans.id1
    update(transactions)
      .set(oid_, trans.oid)
      .set(id1_, transid1)
      .set(store_, trans.store)
      .set(costcenter_, trans.costcenter)
      .set(transdate_, trans.transdate)
      .set(modelid_, trans.modelid)
      .set(company_, trans.company)
      .set(text_, trans.text)
      .set(period_, common.getPeriod(trans.transdate))
      .set(posted_, trans.posted)
      .where((id_ === trans.id) && (company_ === trans.company))
  }
  @nowarn
  override def modify(model:Transaction): ZIO[Any, RepositoryError, Int] = {

      val newLines = model.lines.filter(_.id == -1L).map(l=>l.copy(transid = model.id))
      val deletedLineIds = model.lines.filter(_.transid == -2L).map(line => line.id)
      val oldLines2Update = model.lines.filter(_.id > 0L).map(l=>l.copy(transid = model.id))
      val update_ = build(model)
      val result = for {
        insertedDetails <- ZIO.when(newLines.nonEmpty)(buildInsertNewLines(newLines).map(_.run).flip.map(_.size))
        deletedDetails <- ZIO.when(deletedLineIds.nonEmpty)(buildDeleteDetails(deletedLineIds).run)
        updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(d => buildUpdateDetails(d).run).flip.map(_.sum))
        updatedTransactions <- update_.run
      } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions

      def buildResult =
        transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
        LogStatement(deletedLineIds, oldLines2Update, List(update_)) *> buildResult
    }

  @nowarn
  override def modify(models:List[Transaction]): ZIO[Any, RepositoryError, Int] = {
    val newLines = models.flatMap(m=>m.lines.filter(_.id == -1L).map(l=>l.copy(transid = m.id)))
    val deletedLineIds = models.flatMap(m=>m.lines.filter(_.transid == -2L).map(line => line.id))
    val oldLines2Update = models.flatMap(m=>m.lines.filter(_.id > 0L).map(l=>l.copy(transid = m.id)))
    val update_ = models.map(build)
    val result = for {
      insertedDetails <- ZIO.when(newLines.nonEmpty)(buildInsertNewLines(newLines).map(_.run).flip.map(_.size))
      deletedDetails <- ZIO.when(deletedLineIds.nonEmpty)(buildDeleteDetails(deletedLineIds).run)
      updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(d => buildUpdateDetails(d).run).flip.map(_.sum))
      updatedTransactions <- update_.map(_.run).flip.map(_.size)
    } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions

    def buildResult =
      transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
      LogStatement(deletedLineIds, oldLines2Update, update_) *> buildResult
  }

  private def LogStatement(deletedLineIds: List[Long], oldLines2Update: List[TransactionDetails], trans2Update: List[Update[Transactionx]]) = {
    ZIO.when(oldLines2Update.nonEmpty)(ZIO.logInfo(s"Update lines transaction update stmt ${oldLines2Update.map(buildUpdateDetails).map(renderUpdate)}")) *>
      ZIO.when(deletedLineIds.nonEmpty)(ZIO.logInfo(s"Delete lines transaction  stmt ${renderDelete(buildDeleteDetails(deletedLineIds))}")) *>
      ZIO.logInfo(s"Modify transaction stmt ${trans2Update.map(renderUpdate)}")
  }

  @nowarn
  override def update(model: Transaction): ZIO[Any, RepositoryError, Transaction] =
    if (model.id<=0){
      create(model)
    }else {
    modify(model) *>getByTransId(model.id, model.company)
    }

  override def updatePostedField(models: List[Transaction]): ZIO[Any, RepositoryError, Int] = for {
    nr <- ZIO.foreach(models)(updatePostedField).map(_.sum)
  } yield nr

  @nowarn
  override def updatePostedField(model: Transaction): ZIO[Any, RepositoryError, Int] = {
    val updateSQL = update(transactions).set(posted_, true).where((id_ === model.id) && (company_ === model.company))
    val result = for {
      _<- ZIO.logDebug(s"Query to execute findAll is ${renderUpdate(updateSQL)}")
      m <- updateSQL.run
    } yield m
    transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)
  }
  private[this] def list1(companyId: String): ZStream[Any, RepositoryError, Transaction] = {
    val selectAll = SELECT2.where(company_ === companyId)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[Transaction](c => Transaction.applyC(c)))
        .provideDriver(driverLayer)
  }
  override def all(companyId: String): ZIO[Any, RepositoryError, List[Transaction]] = for {
    trans <- list1(companyId).mapZIO(tr => withLines(tr)).runCollect.map(_.toList)
  } yield trans

  private[this] def find4Period_( fromPeriod: Int, toPeriod: Int, companyId: String
                                ): ZStream[Any, RepositoryError, Transaction] = {
    val selectAll = SELECT2
      .where((company_ === companyId) && (period_ >= fromPeriod) && (period_ <= toPeriod))
      .orderBy(costcenter_.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period1 is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to[Transaction](c => Transaction.applyC(c)))
        .provideDriver(driverLayer)
  }
  def find4Period(fromPeriod: Int, toPeriod: Int, companyId: String
                 ): ZStream[Any, RepositoryError, Transaction] = for {
    trans <- find4Period_(fromPeriod, toPeriod, companyId)
      .mapZIO(tr => getTransWithLines(tr, tr.company))
  } yield trans

  private[this] def getLineByTransId(trans: Transaction): ZStream[Any, RepositoryError, TransactionDetails] = {
    val selectAll = SELECT_LINE.where(transid === trans.id1)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getLineByTransId1 is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(x => TransactionDetails.apply(x)))
        .provideDriver(driverLayer)
  }

  private[this] def getTransWithLines(trans: Transaction, companyId: String): ZIO[Any, RepositoryError, Transaction] = for {
      trans  <- getByTransId_(trans.id, companyId)
      lines_ <- getLineByTransId(trans).runCollect.map(_.toList)
    } yield trans.copy(lines = lines_)//.filter(_.transid==trans.id))

  private[this] def getByTransId_(id: Long, companyId: String): ZIO[Any, RepositoryError, Transaction] = for {
    trans <- getById(id, companyId)
  } yield trans

  override def getByTransId(id:(Long,  String)): ZIO[Any, RepositoryError, Transaction] = for {
    trans  <- getById(id._1, id._2)
    lines_ <- getLineByTransId(trans).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)

   private def getByTransId1x(id: (List[Long], String)): ZIO[Any, RepositoryError, List[Transaction]] = for {
    transactions <- getByIds(id._1, id._2)
    trans <-transactions.map(withLines).flip
   } yield trans

   private def withLines(trans: Transaction): ZIO[Any, RepositoryError, Transaction] =
    getByTransId1((trans.id1, trans.company))


  override def getByTransId1(id: (Long, String)): ZIO[Any, RepositoryError, Transaction] = for {
    trans <- getById1(id._1, id._2)
    lines_ <- getLineByTransId(trans).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)
  def getById(id: Long, companyId: String): ZIO[Any, RepositoryError, Transaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id_ === id))
    ZIO.logDebug(s"Query to execute getById ${id} is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Transaction](c => Transaction.applyC(c)))
        .findFirstLong(driverLayer, id)
  }

  override def getByIds(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[Transaction]] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id_ in  ids))
     ZIO.logDebug(s"Query to execute getByIds ${ids} is ${renderRead(selectAll)}") *>
      execute(selectAll.to[Transaction](c => Transaction.applyC(c)))
        .provideDriver(driverLayer)
        .runCollect.map(_.toList)
  }

  def getById1(id: Long, companyId: String): ZIO[Any, RepositoryError, Transaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id1_ === id))
    ZIO.logInfo(s"Query to execute getById1 ${id} is ${renderRead(selectAll)}") *>
      execute(selectAll.to(x => Transaction.apply(x)))
        .findFirstLong(driverLayer, id)
  }
  override def getByModelId(modelId:(Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = for {
    trans <- getByModelIdX(modelId._1,modelId._2).mapZIO(tr => getTransWithLines(tr, modelId._2)).runCollect.map(_.toList)
  }yield trans

  override def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Transaction] = {
    val selectAll = SELECT2.where((modelid_ === modelId) && (company_ === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelIdX modelid:  ${modelId}  companyId:  ${companyId}is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[Transaction](c => Transaction.applyC(c)))
        .provideDriver(driverLayer)
  }

}

object TransactionRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryImpl(_))
}