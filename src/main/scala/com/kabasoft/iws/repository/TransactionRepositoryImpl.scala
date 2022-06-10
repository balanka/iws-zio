package com.kabasoft.iws.repository

import zio._
import zio.stream._
import zio.sql.ConnectionPool
import com.kabasoft.iws.domain.{ DerivedTransaction, FinancialsTransaction, FinancialsTransactionDetails }
import com.kabasoft.iws.domain.AppError._

final class TransactionRepositoryImpl(pool: ConnectionPool)
    extends TransactionRepository
    with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val LINES =
    lid_ ++ transid ++ laccount_ ++ side_ ++ oaccount_ ++ amount_ ++ duedate_ ++ ltext_ ++ currency_

  val XX =
    tid_ ++ oid_ ++ account_ ++ transdate_ ++ enterdate_ ++ postingdate_ ++ period_ ++ posted_ ++ modelid_ ++ company_ ++ text_ ++ file_content_ ++ lid_ ++ side_ ++ oaccount_ ++ amount_ /*++duedate_*/ ++ currency_ ++ ltext_

  val XX2 =
    tid_ ++ oid_ ++ account_ ++ costcenter ++ transdate_ ++ enterdate_ ++ postingdate_ ++ period_ ++ posted_ ++ modelid_ ++ company_ ++ text_ ++ typeJournal_ ++ file_content_

  private def getQuery4Model(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] = {

    val trans          = models.map(FinancialsTransaction.apply1(_))
    val linesTransData = trans.map(d => d.lines.map(l => FinancialsTransactionDetails.unapply(l).get)).flatten
    val tuples         = trans.map(t => toTupleF(t))
    val query          = insertInto(transaction)(XX2).values(tuples)
    val queryLines     = insertInto(transactionDetails)(LINES).values(linesTransData)

    ZIO.logInfo(
      s"Query to insert TransactionDetails is ${renderInsert(queryLines)}" +
        s"Query to insert Transaction is ${renderInsert(query)}"
    )

    execute(query).provideAndLog(driverLayer)
    // .map(l => l.fold(0)((a, b) => a + b))
    execute(queryLines).provideAndLog(driverLayer)
    // .map(m => m.fold(0)((a, b) => a + b))
  }

  override def create(model: DerivedTransaction): ZIO[Any, RepositoryError, Int] = getQuery4Model(List(model))

  override def create(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] = getQuery4Model(models)

  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(financialstransaction).where((id === item) && (company === companyId)))
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
      .where(lid_ === details.lid)

  private def build(trans: FinancialsTransaction) =
    update(transaction)
      .set(oid_, trans.oid)
      .set(account_, trans.account)
      .set(transdate_, trans.transdate)
      .set(modelid_, trans.modelid)
      .set(company_, trans.company)
      .set(file_content_, trans.file_content)
      .where((tid_ === trans.tid) && (company_ === trans.company))

  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]     = {
    val detailsUpdate = model.lines.map(d => updateDetails(d))
    val update_       = build(model)

    /*for {
     i <- execute(update_).provideLayer(driverLayer)
     j <- executeBatchUpdate(detailsUpdate)
       .provideLayer(driverLayer).map(_.sum)
    }yield i+j
     */

    execute(update_).provideLayer(driverLayer) // *>
    executeBatchUpdate(detailsUpdate)
      .provideLayer(driverLayer)
      .map(_.sum)
      .mapError(e => RepositoryError(e.getCause()))
  }
  override def modify(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] = {
    val trans         = models.map(FinancialsTransaction.apply1)
    val detailsUpdate = trans.flatMap(_.lines).map(updateDetails)
    // val detailsUpdate = details.map(updateDetails(_))
    val update_       = trans.map(build)
    // val result=for{
    executeBatchUpdate(update_).provideLayer(driverLayer) // *>
    executeBatchUpdate(detailsUpdate)
      .provideLayer(driverLayer)
      .map(_.sum)
      .mapError(e => RepositoryError(e.getCause()))

  }

  override def modify(model: DerivedTransaction): ZIO[Any, RepositoryError, Int]          = {
    val trans         = FinancialsTransaction.apply1(model)
    val detailsUpdate = trans.lines.map(updateDetails)
    val update_       = build(trans)
    execute(update_).provideLayer(driverLayer) // *>
    executeBatchUpdate(detailsUpdate)
      .provideLayer(driverLayer)
      .map(_.sum)
      .mapError(e => RepositoryError(e.getCause()))
  }
  override def list(companyId: String): ZStream[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = select(XX)
      .from(transaction.join(transactionDetails).on(transid === tid_))
      .where(company_ === companyId)

    ZStream.fromZIO(ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .provideDriver(driverLayer)
  }
  def find4Period(
    fromPeriod: Int,
    toPeriod: Int,
    companyId: String
  ): ZStream[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = select(XX)
      .from(transaction.join(transactionDetails).on(transid === tid_))
      .where((company_ === companyId) && (period_ >= fromPeriod) && (period_ <= toPeriod))
      .orderBy(account_.descending)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .provideDriver(driverLayer)
  }
  /*
  def find4Period(fromPeriod: Int, toPeriod: Int, companyId: String):ZStream[Any, RepositoryError, FinancialsTransaction] = {
    val selectAll = select(XX)
      .from(transaction.join(transactionDetails).on(transid === tid_))
      .where((company_ === companyId) && (period_ >=  fromPeriod) && (period_ <= toPeriod))
      .orderBy(account_.descending)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute find4Period is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .provideDriver(driverLayer)
  }

   */

  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = select(XX)
      .from(transaction.join(transactionDetails).on(transid === tid_))
      .where((company_ === companyId) && (tid_ === Id.toLong))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .findFirstLong(driverLayer, Id.toLong)
  }

  override def getByModelId(modelId: Int, companyId: String): ZStream[Any, RepositoryError, DerivedTransaction] = {
    val selectAll = select(XX)
      .from(transaction.join(transactionDetails).on(transid === tid_))
      .where((modelid_ === modelId) && (company_ === companyId))
    ZStream.fromZIO(ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((DerivedTransaction.apply _).tupled))
        .provideDriver(driverLayer)
  }
}

object TransactionRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, TransactionRepository] =
    ZLayer.fromFunction(new TransactionRepositoryImpl(_))
}
