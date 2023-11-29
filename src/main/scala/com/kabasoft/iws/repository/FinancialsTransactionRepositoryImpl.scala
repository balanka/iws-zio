package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Account, FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionx, common}
import zio.{ZIO, _}
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

import scala.annotation.nowarn

final class FinancialsTransactionRepositoryImpl(pool: ConnectionPool, accRepo: AccountRepository) extends FinancialsTransactionRepository with TransactionTableDescription {

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

  private val SELECT_LINE = select(lid_, transid, laccount_, side_, oaccount_, amount_, duedate_, ltext_, currency_, accountName_, oaccountName_).from(ftransactionDetails)


  @nowarn
  def create2(transactions: List[FinancialsTransaction], accounts:List[Account]): ZIO[Any, RepositoryError, Int] =
    transact(create2s( buildId1(transactions), accounts))
      .mapError(e => RepositoryError(e.toString))
      .provideLayer(driverLayer)

  @nowarn
  override def create(ftr : FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] =
    if (ftr.id > 0) {
      update(ftr)
    } else {
      val ids =  ftr.lines.map(_.account) ++ftr.lines.map(_.oaccount)
      val trs = for {
        accounts <- accRepo.getBy(ids, ftr.company)
        transactions = buildId1(List(ftr))
        nr <-  create2(transactions, accounts) *> getByTransId1(transactions.head.id1, ftr.company)
      } yield nr
      trs
    }


  @nowarn
  override def create(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = {
    var company = ""
    val ids = models.flatMap(tr=> {company=tr.company; tr.lines.map(_.account)})++models.flatMap(tr=>tr.lines.map(_.oaccount))
    val trs =for{
      accounts        <-  accRepo.getBy(ids, company)
      transactions = buildId1(models)
     nr <-  create2(transactions, accounts) *>
        getByTransId1x(transactions.map(m => m.id1), transactions.head.company)
    }yield nr
    trs
  }

  private def buildDeleteDetails(ids: List[Long]): Delete[FinancialsTransactionDetails] =
    deleteFrom(ftransactionDetails).where(lid_ in ids)

  override def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val deleteQuery = deleteFrom(transactions).where((id_ === id) && (company_ === companyId))
    ZIO.logDebug(s"Delete  FinancialsTransactionDetails  ${renderDelete(deleteQuery)}")*>
      execute(deleteQuery)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  private def buildUpdateDetails(details: FinancialsTransactionDetails, accounts:List[Account]): Update[FinancialsTransactionDetails] = {
    val acc = accounts.find(acc=>acc.id==details.account)
    val oacc = accounts.find(acc=>acc.id==details.oaccount)
    update(ftransactionDetails)
      .set(transid, details.transid)
      .set(laccount_, details.account)
      .set(accountName_, acc.fold(details.accountName)(acc=>acc.name))
      .set(oaccount_, details.oaccount)
      .set(oaccountName_, oacc.fold(details.oaccountName)(acc=>acc.name))
      .set(side_, details.side)
      .set(amount_, details.amount)
      .set(duedate_, details.duedate)
      .set(ltext_, details.text)
      .set(currency_, details.currency)
      .where(lid_ === details.id)
  }

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
      .set(period_, common.getPeriod(trans.transdate))
      .set(posted_, trans.posted)
      .set(type_journal_, trans.typeJournal)
      .set(file_content_, trans.file_content)
      .where((id_ === trans.id) && (company_ === trans.company))
  }

  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    models.map(modify).flip.map(_.sum)

  @nowarn
  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {

      val newLines = model.lines.filter(_.id == -1L).map(l=>l.copy(transid = model.id))
      val deletedLineIds = model.lines.filter(_.transid == -2L).map(line => line.id)
      val oldLines2Update = model.lines.filter(_.id > 0L).map(l=>l.copy(transid = model.id))
      val update_ = build(model)
      val ids     = newLines.map(_.account)++newLines.map(_.oaccount)++oldLines2Update.map(_.account)++oldLines2Update.map(_.oaccount)
      val result = for {
        accounts        <-  accRepo.getBy(ids, model.company)
        insertedDetails <- ZIO.when(newLines.nonEmpty)(buildInsertNewLines(newLines, accounts).map(_.run).flip.map(_.size))
        deletedDetails <- ZIO.when(deletedLineIds.nonEmpty)(buildDeleteDetails(deletedLineIds).run)
        updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(d => buildUpdateDetails(d, accounts).run).flip.map(_.sum))
        updatedTransactions <- update_.run
        _<-ZIO.logInfo(s"Update lines transaction update stmt ${oldLines2Update.map(buildUpdateDetails(_, accounts)).map(renderUpdate)}")
      } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions

      def buildResult = transact(result).mapError(e => RepositoryError(e.toString)).provideLayer(driverLayer)

      //ZIO.logInfo(s"New lines transaction insert stmt ${buildInsertNewLines(newLines).map(renderInsert)}") *>
        //ZIO.logInfo(s"Update lines transaction update stmt ${oldLines2Update.map(buildUpdateDetails(_, accounts)).map(renderUpdate)}") *>
        ZIO.logInfo(s"Delete lines transaction  stmt ${renderDelete(buildDeleteDetails(deletedLineIds))}") *>
        ZIO.logInfo(s"Modify transaction stmt ${renderUpdate(update_)}") *> buildResult
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
  def find4Period(fromPeriod: Int, toPeriod: Int, companyId: String
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
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .findFirstLong(driverLayer, id)
  }

  override def getByIds(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id1_ in  ids))
    ZIO.logInfo(s"Query to execute getByIds ${ids} is ${renderRead(selectAll)}") *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
        .runCollect.map(_.toList)
  }

  def getById1(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id1_ === id))
    ZIO.logDebug(s"Query to execute getById1 ${id} is ${renderRead(selectAll)}") *>
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

object FinancialsTransactionRepositoryImpl {
  val live: ZLayer[ConnectionPool with AccountRepository, Throwable, FinancialsTransactionRepository] =
    ZLayer.fromFunction(new FinancialsTransactionRepositoryImpl(_, _))
}