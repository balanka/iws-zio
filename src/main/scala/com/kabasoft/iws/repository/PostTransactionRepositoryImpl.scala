package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.journal_Schema
import com.kabasoft.iws.repository.Schema.pacSchema
import com.kabasoft.iws.domain.{FinancialsTransaction, Journal, Journal_, PeriodicAccountBalance}
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.{ZIO, _}

import scala.annotation.nowarn


final class PostTransactionRepositoryImpl(pool: ConnectionPool) extends PostTransactionRepository with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val pac = defineTable[PeriodicAccountBalance]("periodic_account_balance")
  val journals_ = defineTable[Journal_]("journal")
  val (id_pac, account_pac, period_pac, idebit_pac, icredit_pac, debit_pac, credit_pac, currency_pac, company_pac, name_pac, modelid_pac) = pac.columns
  private val (
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


  private def createPacs4T(models_ : List[PeriodicAccountBalance]): ZIO[SqlTransaction, Exception, Int] = {
        insertInto(pac)(id_pac, account_pac, period_pac, idebit_pac, icredit_pac, debit_pac, credit_pac,
          currency_pac, company_pac, name_pac, modelid_pac).values(models_.map(c => PeriodicAccountBalance.unapply(c).get)).run
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

   def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val deleteQuery = deleteFrom(transactions).where((id_ === id) && (company_ === companyId))
    ZIO.logDebug(s"Delete  FinancialsTransactionDetails  ${renderDelete(deleteQuery)}")*>
      execute(deleteQuery)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  @nowarn
  override  def post(models: List[FinancialsTransaction], pac2Insert:List[PeriodicAccountBalance], pac2update:UIO[List[PeriodicAccountBalance]],
                     journals:List[Journal]): ZIO[Any, RepositoryError, Int] =  for {
    pac2updatex<-pac2update
    _ <- ZIO.logDebug(s" New Pacs  to insert into DB ${pac2Insert}")
    _ <- ZIO.logDebug(s" Old Pacs  to update in DB ${pac2updatex}")
    _ <- ZIO.logDebug(s" Transaction posted  ${models}")
     z = ZIO.when(models.nonEmpty)(updatePostedField4T(models))
             .zipWith(ZIO.when(pac2Insert.nonEmpty)(createPacs4T(pac2Insert)))((i1, i2)=>i1.getOrElse(0) +i2.getOrElse(0))
             .zipWith(ZIO.when(pac2updatex.nonEmpty)(modifyPacs4T(pac2updatex)))((i1, i2)=>i1 +i2.getOrElse(0))
             .zipWith(ZIO.when(journals.nonEmpty)(createJ4T(journals)))((i1, i2)=>i1 +i2.getOrElse(0))

    nr<-  transact(z).mapError(e=>RepositoryError(e.getMessage)).provideLayer(driverLayer)
  }yield nr//transact(z).mapError(e=>RepositoryError(e.getMessage)).provideLayer(driverLayer)

  private def  updatePostedField4T(models: List[FinancialsTransaction]): ZIO[SqlTransaction, Exception, Int] = {
    val updateSQL = models.map(model =>
                 update(transactions).set(posted_, true).where((id_ === model.id) && (company_ === model.company)))
    val result = for {
      _ <- ZIO.logDebug(s"Query to execute findAll is ${updateSQL.map(renderUpdate)}")
      m <- updateSQL.map(_.run).flip.map(_.sum)
    } yield m
    result
  }



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
      c.modelid
    )


  }
object PostTransactionRepositoryImpl {

  val live: ZLayer[ConnectionPool, RepositoryError, PostTransactionRepository] =
    ZLayer.fromFunction(new PostTransactionRepositoryImpl(_))
}
