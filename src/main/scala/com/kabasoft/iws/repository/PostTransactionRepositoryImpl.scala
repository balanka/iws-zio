package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.journal_Schema
import com.kabasoft.iws.repository.Schema.pacSchema
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails, FinancialsTransactionx, Journal, Journal_, PeriodicAccountBalance}
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.{ZIO, _}

import java.time.Instant
import scala.annotation.nowarn


final class PostTransactionRepositoryImpl(pool: ConnectionPool) extends PostTransactionRepository with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val pac = defineTable[PeriodicAccountBalance]("periodic_account_balance")
  val journals_ = defineTable[Journal_]("journal")
  val (id_pac, account_pac, period_pac, idebit_pac, icredit_pac, debit_pac, credit_pac, currency_pac, company_pac, modelid_pac) = pac.columns
  val (
    transid_j,
    oid_j,
    account_j,
    oaccount_j,
    transdate_j,
    enterdate_j,
    postingdate_j,
    period_j,
    amount_j,
    idebit_j,
    debit_j,
    icredit_j,
    credit_j,
    currency_j,
    side_j,
    text_j,
    month_j,
    year_j,
    company_j,
    modelid_j
    ) = journals_.columns

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

 private def whereClause(idx: String, companyId: String) =
   List(id_pac === idx, company_pac === companyId)
     .fold(Expr.literal(true))(_ && _)

  private def insertNewLines(models: List[FinancialsTransactionDetails])=
   insertInto(transactionDetailsInsert)(transidx,  laccountx, sidex, oaccountx, amountx, duedatex, ltextx, currencyx)
     .values(models.map(toTuple))

  private def buildInsertQuery(model: FinancialsTransaction) =
    insertInto(transactionInsert)(
      oidx,
      id1,
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
    ).values(toTupleC(model))

//  private def buildInsertQuery(models: List[FinancialsTransaction])=
//    insertInto(transactionInsert)(
//      oidx,
//      id1,
//      costcenterx,
//      accountx,
//      transdatex,
//      enterdatex,
//      postingdatex,
//      periodx,
//      postedx,
//      modelidx,
//      companyx,
//      textx,
//      type_journalx,
//      file_contentx
//    ).values( models.map(toTupleC))

//  private def createJ4T(c: Journal): ZIO[SqlTransaction, Exception, Int]                 = {
//     insertInto(journals_)(
//       transid_j,
//       oid_j,
//       account_j,
//       oaccount_j,
//       transdate_j,
//       enterdate_j,
//       postingdate_j,
//       period_j,
//       amount_j,
//       idebit_j,
//       debit_j,
//       icredit_j,
//       credit_j,
//       currency_j,
//       side_j,
//       text_j,
//       month_j,
//       year_j,
//       company_j,
//       modelid_j
//    )
//      .values(tuple2(c)).run
//  }


//  private def createPac4T(model : PeriodicAccountBalance): ZIO[SqlTransaction, Exception, Int] = createPacs4T(List(model))
//  private def modifyPac4T(model: PeriodicAccountBalance) = buildPac4T(model).run



//  private def create4T(models : List[FinancialsTransaction]): ZIO[SqlTransaction, Exception, Int] = {
//      var time = Instant.now().getEpochSecond
//      time *= 1000000000L //convert to nanoseconds
//      val transid1 = time & ~9223372036854251520L
//      val modelsx = models.map(model=>model.copy(id1 = transid1, lines = model.lines.map(_.copy(transid = transid1))))
//      val insertNewLines_ = insertNewLines(models.flatMap(_.lines))
//      val trans: ZIO[SqlTransaction, Exception, Int] = for {
//        x <- buildInsertQuery(modelsx).run
//        _ <- ZIO.logInfo(s"Create transaction stmt   ${renderInsert(buildInsertQuery(models))} ")
//        y <- insertNewLines_.run
//        _ <- ZIO.logInfo(s"Create line transaction stmt   ${renderInsert(insertNewLines_)} ")
//      } yield x + y
//      trans
//    }

  //  @nowarn
  //  override def post(model: FinancialsTransaction, pac2Insert: PeriodicAccountBalance, pac2update: PeriodicAccountBalance,
  //                    journal: Journal): ZIO[Any, RepositoryError, Int] = {
  //    val z = modify4T(model)
  //      .zip(createJ4T(journal))
  //      .zip(createPac4T(pac2Insert))
  //      .zip(modifyPac4T(pac2update))
  //      .map(i => i._1 + i._2 + i._3 + i._4 )
  //    transact(z).mapError(e=>RepositoryError(e.getMessage)).provideLayer(driverLayer)
  //  }

  //  private def modify4T(models: List[FinancialsTransaction]): ZIO[SqlTransaction, Exception, Int] = {
  //      val newLines = models.flatMap(_.lines.filter(_.id == -1L))
  //      val deletedLineIds = models.flatMap(_.lines.filter(_.transid == -2L).map(line => line.id))
  //      val oldLines2Update = models.map(model =>model.lines.filter(l => l.transid == model.id && (l.id > 0L)))
  //      //val update_ = models.map(model=>build(model).run)
  //
  //      val result: ZIO[SqlTransaction, Exception, Int] = for {
  //        insertedDetails <- ZIO.when(newLines.nonEmpty)(insertNewLines(newLines).run)
  //        deletedDetails <- ZIO.when(deletedLineIds.nonEmpty)(buildDeleteDetails(deletedLineIds).run)
  //        updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.flatMap(d => d.map(buildUpdateDetails_)).map(_.run).flip.map(_.sum))
  //        updatedTransactions <- models.map(model=>build(model)).map(_.run).flip.map(_.sum)
  //      } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions
  //      result
  //    }

    private def createPacs4T(models_ : List[PeriodicAccountBalance]): ZIO[SqlTransaction, Exception, Int] = {
        insertInto(pac)(id_pac, account_pac, period_pac, idebit_pac, icredit_pac, debit_pac, credit_pac,
          currency_pac, company_pac, modelid_pac).values(models_.map(c => PeriodicAccountBalance.unapply(c).get)).run
    }

  private def createJ4T(journals: List[Journal]): ZIO[SqlTransaction, Exception, Int] = {
    insertInto(journals_)(
      transid_j,
      oid_j,
      account_j,
      oaccount_j,
      transdate_j,
      enterdate_j,
      postingdate_j,
      period_j,
      amount_j,
      idebit_j,
      debit_j,
      icredit_j,
      credit_j,
      currency_j,
      side_j,
      text_j,
      month_j,
      year_j,
      company_j,
      modelid_j
    )
      .values(journals.map(tuple2)).run
  }
  private def modifyPacs4T(models: List[PeriodicAccountBalance]) = models.map(buildPac4T).map(_.run).flip.map(_.sum)

  private def buildPac4T(model: PeriodicAccountBalance): Update[PeriodicAccountBalance] =
    update(pac)
      .set(idebit_pac, model.idebit)
      .set(debit_pac, model.debit)
      .set(icredit_pac, model.icredit)
      .set(credit_pac, model.credit)
      //.set(currency, model.currency)
      //.set(company, model.company)
      .where(whereClause(model.id, model.company))

  private def create4T(model_ : FinancialsTransaction): ZIO[SqlTransaction, Exception, Int] = {
    if (model_.id > 0) {
      modify4T(model_)
    } else {
      var time = Instant.now().getEpochSecond
      time *= 1000000000L //convert to nanoseconds
      val transid1 = time & ~9223372036854251520L
      val model = model_.copy(id1 = transid1, lines = model_.lines.map(_.copy(transid = transid1)))
      val insertNewLines_ = insertNewLines(model.lines)
      val trans: ZIO[SqlTransaction, Exception, Int] = for {
        x <- buildInsertQuery(model).run
        _ <- ZIO.logInfo(s"Create transaction stmt   ${renderInsert(buildInsertQuery(model))} ")
        y <- insertNewLines_.run
        _ <- ZIO.logInfo(s"Create line transaction stmt   ${renderInsert(insertNewLines_)} ")
      } yield x + y
      trans
    }

  }

  private def buildDeleteDetails(ids: List[Long]): Delete[FinancialsTransactionDetails] =
    deleteFrom(transactionDetails).where(lid_ in ids)

   def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
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

  @nowarn
  override  def post(models: List[FinancialsTransaction], pac2Insert:List[PeriodicAccountBalance], pac2update:List[PeriodicAccountBalance],
                     journals:List[Journal]): ZIO[Any, RepositoryError, Int] =  {
     val z = ZIO.when(models.nonEmpty)(updatePostedField4T(models))
             .zipWith(ZIO.when(pac2Insert.nonEmpty)(createPacs4T(pac2Insert)))((i1, i2)=>i1.getOrElse(0) +i2.getOrElse(0))
             .zipWith(ZIO.when(pac2update.nonEmpty)(modifyPacs4T(pac2update)))((i1, i2)=>i1 +i2.getOrElse(0))
             .zipWith(ZIO.when(journals.nonEmpty)(createJ4T(journals)))((i1, i2)=>i1 +i2.getOrElse(0))

   transact(z).mapError(e=>RepositoryError(e.getMessage)).provideLayer(driverLayer)
  }

  private def  updatePostedField4T(models: List[FinancialsTransaction]): ZIO[SqlTransaction, Exception, Int] = {
    val updateSQL = models.map(model =>
                 update(transactions).set(posted_, true).where((id_ === model.id) && (company_ === model.company)))
    val result = for {
      _ <- ZIO.logDebug(s"Query to execute findAll is ${updateSQL.map(renderUpdate)}")
      m <- updateSQL.map(_.run).flip.map(_.sum)
    } yield m
    result
  }

  private def modify4T(model: FinancialsTransaction): ZIO[SqlTransaction, Exception, Int] = {
    if (model.id <= 0) {
      create4T(model)
    } else {
      val newLines = model.lines.filter(_.id == -1L)
      val deletedLineIds = model.lines.filter(_.transid == -2L).map(line => line.id)
      val oldLines2Update = model.lines.filter(l => l.transid == model.id && (l.id > 0L))
      val update_ = build(model)

      val result: ZIO[SqlTransaction, Exception, Int] = for {
        insertedDetails <- ZIO.when(newLines.nonEmpty)(insertNewLines(newLines).run)
        deletedDetails <- ZIO.when(deletedLineIds.nonEmpty)(buildDeleteDetails(deletedLineIds).run)
        updatedDetails <- ZIO.when(oldLines2Update.nonEmpty)(oldLines2Update.map(d => buildUpdateDetails_(d).run).flip.map(_.sum))
        updatedTransactions <- update_.run
      } yield insertedDetails.getOrElse(0) + deletedDetails.getOrElse(0) + updatedDetails.getOrElse(0) + updatedTransactions
      result
    }
  }

  @nowarn
  override def modifyT(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] =
    transact(modify4T(model)).mapError(e=>RepositoryError(e.getMessage)).provideLayer(driverLayer)

    def tuple2(c: Journal) = (
      c.transid,
      c.oid,
      c.account,
      c.oaccount,
      c.transdate,
      c.enterdate,
      c.postingdate,
      c.period,
      c.amount,
      c.idebit,
      c.debit,
      c.icredit,
      c.credit,
      c.currency,
      c.side,
      c.text,
      c.month,
      c.year,
      c.company,
      // c.file,
      c.modelid
    )


  }
object PostTransactionRepositoryImpl {

  val live: ZLayer[ConnectionPool, RepositoryError, PostTransactionRepository] =
    ZLayer.fromFunction(new PostTransactionRepositoryImpl(_))
}
