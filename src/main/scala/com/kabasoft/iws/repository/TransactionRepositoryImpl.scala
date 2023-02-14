package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails}
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class TransactionRepositoryImpl(pool: ConnectionPool) extends TransactionRepository with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val SELECT2                                                                                   =
    select(id_, oid_, account_, costcenter, transdate_, enterdate_, postingdate_, period_, posted_, modelid_, company_
      , text_, type_journal, file_content_)
    .from(transaction)
  val SELECT                                                                                   =
    select(id_, oid_, costcenter, account_, transdate_, enterdate_, postingdate_, period_, posted_, modelid_, company_,
      text_, type_journal, file_content_, lid_, laccount_, side_, oaccount_, amount_, duedate_, currency_, ltext_)
      .from(transaction.leftOuter(transactionDetails).on(transid === id_))
  val SELECTD = select(id_, oid_, account_, transdate_, enterdate_, postingdate_, period_, posted_, modelid_, company_
    , text_, file_content_, lid_, side_, oaccount_, amount_ , currency_, ltext_)
    .from(transaction.join(transactionDetails).on(transid === id_))
  val SELECT_LINE                                                                              = select(lid_, transid, laccount_, side_,
    oaccount_, amount_, duedate_, ltext_, currency_).from(transactionDetails)
  val CURRVAL                         = FunctionDef[String, Long](FunctionName("currval"))

  private def getLastTransid: ZIO[Any, RepositoryError, Option[Long]] = {
    val selectAll = select(CURRVAL("master_compta_id_seq"))
    execute(selectAll).runHead.provideAndLog(driverLayer)
   //.execute(selectAll).runHead
  }

  private def getQuery4Model(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] = {
    val trans          = FinancialsTransaction.applyD(models.map(DerivedTransaction.unapply(_).get))
    val linesTransData = trans.map(d => d.lines.map(l => FinancialsTransactionDetails.unapply(l).get)).flatten
    val tuples         = trans.map(t => toTupleF(t))
    val query          = insertInto(transaction)(id_, oid_, account_, costcenter, transdate_, enterdate_, postingdate_
      , period_, posted_, modelid_, company_, text_, type_journal, file_content_).values(tuples)
    val queryLines     = insertInto(transactionDetails)(lid_, transid, laccount_, side_,
      oaccount_, amount_, duedate_, ltext_, currency_).values(linesTransData)

    ZIO.logDebug(
      s"Query to insert TransactionDetails is ${renderInsert(queryLines)}  " +
        s"Transaction is ${renderInsert(query)}")

    val result = for {
      r1 <- execute(query).provideAndLog(driverLayer)
      r2 <- execute(queryLines).provideAndLog(driverLayer)
    } yield  (r1 + r2)

    result
  }

  private def getQuery4Model2(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = {
    val tuples = models.map(t => toTupleC(t))
   val  query = insertInto(transaction2)(oidx, costcenterx, accountx, transdatex, enterdatex, postingdatex, periodx
     , postedx, modelidx, companyx, textx, type_journalx, file_contentx).values(tuples)
    val result         = for {
      _ <-  ZIO.logDebug(s"Query to insert Transaction is ${renderInsert(query)}")
      r1 <- execute(query).provideAndLog(driverLayer)
      transId <- getLastTransid
      _ <- ZIO.logDebug(s"Next transaction id is ${transId}")
      linesTransData = models.map(d => d.lines.map(l => toTupleC(l.copy(transid = transId.getOrElse(-1L))))).flatten
      queryLines     = insertInto(transactionDetails_)(transidx, laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx).values(linesTransData)
      _ <- ZIO.logDebug(s"Query to insert Libes Transaction is ${renderInsert(queryLines)}")
      r2 <- execute(queryLines).provideAndLog(driverLayer)
    } yield (r1 + r2)
    result
  }

  override def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]                = getQuery4Model2(List(model))
  override def create(model: DerivedTransaction): ZIO[Any, RepositoryError, Int]                   = getQuery4Model(List(model))
  override def create(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] = getQuery4Model(models)

  override def delete(Id: Long, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(financialstransaction).where((id === Id) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))
  private def updateDetails(
                             details: FinancialsTransactionDetails
                           ): Update[TransactionRepositoryImpl.this.transactionDetails.TableType] =
    update(transactionDetails)
      .set(transid, details.transid)
      .set(laccount_, details.account)
      .set(side_, details.side)
      .set(amount_, details.amount)
      .set(duedate_, details.duedate)
      .set(ltext_, details.text)
      .set(currency_, details.currency)
      .where(lid_ === details.id)

  private def build(trans: FinancialsTransaction) =
    update(transaction)
      .set(oid_, trans.oid)
      .set(costcenter, trans.costcenter)
      .set(account_, trans.account)
      .set(transdate_, trans.transdate)
      .set(modelid_, trans.modelid)
      .set(company_, trans.company)
      .set(text_, trans.text)
      .set(type_journal,trans.typeJournal)
      .set(file_content_, trans.file_content)
      .where((id_ === trans.id) && (company_ === trans.company))

  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = {
    val detailsUpdate = model.lines.map(d => updateDetails(d))
    val update_       = build(model)

    val result: ZIO[Any, RepositoryError, Int] = for {
      r2 <- executeBatchUpdate(detailsUpdate)
        .provideLayer(driverLayer)
        .map(_.sum)
        .mapError(e => RepositoryError(e.getCause))
      r1 <- execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause))
    } yield r1 + r2
    result
  }

  override def modify(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] = {
    val trans         = models.map(FinancialsTransaction.apply1)
    val detailsUpdate = trans.flatMap(_.lines).map(updateDetails)
    val update_       = trans.map(build)
    for {
      r2 <- executeBatchUpdate(detailsUpdate)
        .provideLayer(driverLayer)
        .map(_.sum)
        .mapError(e => RepositoryError(e.getCause))
      r1 <- executeBatchUpdate(update_)
        .provideLayer(driverLayer)
        .map(_.sum)
        .mapError(e => RepositoryError(e.getCause))
    } yield r2 + r1

  }

  override def modify(model: DerivedTransaction): ZIO[Any, RepositoryError, Int] = {
    val trans         = FinancialsTransaction.apply1(model)
    val detailsUpdate = trans.lines.map(updateDetails)
    val update_       = build(trans)
    for {
      r2 <- executeBatchUpdate(detailsUpdate)
        .provideLayer(driverLayer)
        .map(_.sum)
        .mapError(e => RepositoryError(e.getCause))
      r1 <- execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause))
    } yield r1 + r2
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = SELECTD.where(company_ === companyId)

    ZStream.fromZIO(ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .provideDriver(driverLayer)
  }
  private[this] def list1(companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where(company_ === companyId)

    ZStream.fromZIO(ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")) *>
      execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.applyC(c)))
        .provideDriver(driverLayer)
  }

  override def all(companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction] = for {
    trans  <- list1(companyId).mapZIO( tr =>getTransWithLines(tr.id, companyId))
  } yield trans

  def find4Period1(
                    fromPeriod: Int,
                    toPeriod: Int,
                    companyId: String
                  ): ZStream[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = SELECTD
      .where((company_ === companyId) && (period_ >= fromPeriod) && (period_ <= toPeriod))
      .orderBy(account_.descending)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .provideDriver(driverLayer)
  }

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
    trans  <- find4Period_(fromPeriod, toPeriod, companyId)
      .mapZIO( tr=>getTransWithLines(tr.id, tr.company))
  } yield trans

  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = SELECTD.where((company_ === companyId) && (id_ === Id.toLong))
    ZIO.logDebug(s"Query to execute findBy ${Id} is ${renderRead(selectAll)}") *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .findFirstLong(driverLayer, 1L)
  }

  private[this] def getLineByTransId(id: Long): ZStream[Any, RepositoryError, FinancialsTransactionDetails] = {
    val selectAll = SELECT_LINE.where(transid === id)
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getLineByTransId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(x => FinancialsTransactionDetails.apply(x)))
        .provideDriver(driverLayer)
  }

  private[this] def getTransWithLines(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans <- getByTransId_(id, companyId)
    lines_ <- getLineByTransId(id).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)

  private[this] def getByTransId_(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans <- getById(id, companyId)
  } yield trans

  override def getByTransId(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    trans  <- getById(id, companyId)
    lines_ <- getLineByTransId(id).runCollect.map(_.toList)
  } yield trans.copy(lines = lines_)

  private def getById(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((company_ === companyId) && (id_ === id))
    ZIO.logDebug(s"Query to execute getById ${id} is ${renderRead(selectAll)}") *>
      execute(selectAll.to(x => FinancialsTransaction.apply(x)))
        .findFirstLong(driverLayer, id)
  }

  override def getByModelId(modelId: Int, companyId: String): ZStream[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = SELECTD.where((modelid_ === modelId) && (company_ === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .provideDriver(driverLayer)
  }

  override def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = SELECT2.where((modelid_ === modelId) && (company_ === companyId))
    execute(selectAll.to[FinancialsTransaction](c => FinancialsTransaction.apply(c)))
      .provideDriver(driverLayer)
      .mapZIO( tr =>getTransWithLines(tr.id, companyId))

  }

}

object TransactionRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryImpl(_))
}