package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Article, Article_, Journal, Journal_, PeriodicAccountBalance, Stock, Transaction}
import com.kabasoft.iws.repository.Schema.{article_Schema, journal_Schema, pacSchema, stockSchema}
import zio.prelude.FlipOps
import zio.prelude.data.Optional.AllValuesAreNullable
import zio.sql.ConnectionPool
import zio.{ZIO, _}

import scala.annotation.nowarn

final class PostTransactionRepositoryImpl(pool: ConnectionPool) extends PostTransactionRepository with TransactionTableDescription {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val pac = defineTable[PeriodicAccountBalance]("periodic_account_balance")
  val stock = defineTable[Stock]("stock")
  val journals_ = defineTable[Journal_]("journal")
  val articles = defineTable[Article_]("article")
  val (id_a, _, _, _, sprice, pprice, avgPrice, _, _, _, _, _, _, company_a, _, _, _, _) = articles.columns
  val (id_st, store_st, article_st, quantity_st, charge_st, company_st,  modelid_st) = stock.columns
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
      store_,
      account_,
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

 private def whereClause(idx: String, companyId: String) =
   List(id_pac === idx, company_pac === companyId)
     .fold(Expr.literal(true))(_ && _)

  def whereClause(storeId: String, articleId: String, companyId:String) =
    List((store_st === storeId), (article_st === articleId), (company_st === companyId))
      .fold(Expr.literal(true))(_ && _)

  private def createPacs4T(models_ : List[PeriodicAccountBalance]): ZIO[SqlTransaction, Exception, Int] = {
        insertInto(pac)(id_pac, account_pac, period_pac, idebit_pac, icredit_pac, debit_pac, credit_pac,
          currency_pac, company_pac, name_pac, modelid_pac).values(models_.map(c => PeriodicAccountBalance.unapply(c).get)).run
    }

  private def createStock4T(models_ : List[Stock]): ZIO[SqlTransaction, Exception, Int] = {
    insertInto(stock)(id_st, store_st, article_st, quantity_st, charge_st, company_st,  modelid_st).values(models_.map(c =>
      Stock.unapply(c).get)).run
  }

  private def buildJ4T(journal: Journal) = {
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
    ).values(tuple2(journal)).run
  }
  private def createJ4T(journals: List[Journal]): ZIO[SqlTransaction, Exception, Int] = journals.map(buildJ4T).flip.map(_.sum)
  private def modifyPacs4T(models: List[PeriodicAccountBalance]) = models.map(buildPac4T).map(_.run).flip.map(_.sum)
  private def modifyStock4T(stock: List[Stock]) = stock.map(buildStock4T).map(_.run).flip.map(_.sum)
  private def modifyPrices4T(articles: List[Article]) = articles.map(Article_.apply).map(buildUpdatePrices).map(_.run).flip.map(_.sum)
  private def buildPac4T(model: PeriodicAccountBalance): Update[PeriodicAccountBalance] =
    update(pac)
      .set(idebit_pac, model.idebit)
      .set(debit_pac, model.debit)
      .set(icredit_pac, model.icredit)
      .set(credit_pac, model.credit)
      .where(whereClause(model.id, model.company))

  private def buildStock4T(model: Stock): Update[Stock] =
    update(stock)
      .set(quantity_st, model.quantity)
      .where(whereClause(model.store, model.article, model.company))

  private def buildUpdatePrices(model: Article_): Update[Article_] =
    update(articles)
      .set(sprice, model.sprice)
      .set(pprice, model.pprice)
      .set(avgPrice, model.avgPrice)
      .where((id_a === model.id) && (company_a === model.company))
      //.where(whereClause(model.id, model.company))

   def delete(id : Long, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val deleteQuery = deleteFrom(transactions).where((id_ === id) && (company_ === companyId))
    ZIO.logDebug(s"Delete  FinancialsTransactionDetails  ${renderDelete(deleteQuery)}")*>
      execute(deleteQuery)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  @nowarn
  override  def post(models: List[Transaction], pac2Insert:List[PeriodicAccountBalance], pac2update:UIO[List[PeriodicAccountBalance]],
                     journals:List[Journal], stocks:List[Stock], newStock:List[Stock], articles:List[Article]): ZIO[Any, RepositoryError, Int] =  for {
    pac2updatex<-pac2update
    _ <- ZIO.when(pac2Insert.nonEmpty)(ZIO.logInfo(s" New Pacs  to insert into DB ${pac2Insert}"))
    _ <- ZIO.when(pac2updatex.nonEmpty)(ZIO.logInfo(s" Old Pacs  to update in DB ${pac2updatex}"))
    _ <- ZIO.when(journals.nonEmpty)(ZIO.logInfo(s" journals  ${journals}"))
    _ <- ZIO.logInfo(s" Transaction posted  ${models}")
     z = ZIO.when(models.nonEmpty)(updatePostedField4T(models))
             .zipWith(ZIO.when(pac2Insert.nonEmpty)(createPacs4T(pac2Insert)))((i1, i2)=>i1.getOrElse(0) +i2.getOrElse(0))
             .zipWith(ZIO.when(pac2updatex.nonEmpty)(modifyPacs4T(pac2updatex)))((i1, i2)=>i1 +i2.getOrElse(0))
             .zipWith(ZIO.when(journals.nonEmpty)(createJ4T(journals)))((i1, i2)=>i1 +i2.getOrElse(0))
             .zipWith(ZIO.when(newStock.nonEmpty)(createStock4T(newStock)))((i1, i2)=>i1.getOrElse(0) +i2.getOrElse(0))
             .zipWith(ZIO.when(stocks.nonEmpty)(modifyStock4T(stocks)))((i1, i2)=>i1.getOrElse(0) +i2.getOrElse(0))
             .zipWith(ZIO.when(articles.nonEmpty)(modifyPrices4T(articles)))((i1, i2)=>i1.getOrElse(0) +i2.getOrElse(0)).debug(s"Article>> ${articles}")

    nr<-  transact(z).mapError(e=>RepositoryError(e.getMessage)).provideLayer(driverLayer)
  }yield nr

  private def  updatePostedField4T(models: List[Transaction]): ZIO[SqlTransaction, Exception, Int] = {
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
