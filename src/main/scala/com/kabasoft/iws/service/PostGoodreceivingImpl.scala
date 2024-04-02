package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.repository._
import zio._
import zio.prelude.FlipOps

import java.math.RoundingMode

final class PostGoodreceivingImpl(pacRepo: PacRepository, accRepo: AccountRepository, artRepo: ArticleRepository
                                  , stockRepo: StockRepository, repository4PostingTransaction:PostTransactionRepository)
                                    extends PostGoodreceiving {

  override def postAll(transactions: List[Transaction]): ZIO[Any, RepositoryError, Int]  =
    for {
      _ <-ZIO.foreachDiscard(transactions.map(_.id)) (
        id =>ZIO.logInfo(s"Posting Goodreceiving transaction  with id ${id} of company ${transactions.head.company}"))
       stockIds = Stock.create(transactions).map(_.id).distinct
      oldStocks <-  stockRepo.getById(stockIds)
      newStock <-  buildNewStock(transactions, oldStocks ).flip
      post <-  postTransaction(transactions, transactions.head.company, newStock, oldStocks)
     nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8)
    } yield nr

  private[this] def postTransaction(transactions: List[Transaction], company: String, newStock:List[Stock], oldStocks:List[Stock]):
  ZIO[Any, RepositoryError, (List[Transaction], List[PeriodicAccountBalance], ZIO[Any, Nothing, List[PeriodicAccountBalance]],
                             List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {

    accounts <- accRepo.all(Account.MODELID, company)
    articles <- artRepo.getBy(transactions.flatMap(m => m.lines.map(_.article)), company)
    accountIds = articles.map(art => (art.stockAccount, art.expenseAccount))
    pacids = accountIds.flatMap(id => transactions.map(tr => buildPacId(tr.period, id))).flatten
    pacs <- pacRepo.getByIds(pacids, company).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
    allPacs = transactions.flatMap(tr => buildPacsFromTransaction(tr, articles, accounts))
    newRecords = allPacs.filterNot(pac => pacs.map(_.id).contains(pac.id))
      .groupBy(_.id) map { case (_, v) => common.reduce(v, PeriodicAccountBalance.dummy) }
    tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
    oldPacs <- updatePac(allPacs, tpacs).map(e => e.map(PeriodicAccountBalance.applyT))
    journalEntries <- makeJournal(transactions, newRecords.toList, oldPacs, articles)
    stocks <- updateStock(transactions, oldStocks)
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
    updatedArticle <- updateAvgPrice(transactions, stocks, articles)
  } yield (transactions, newRecords.toList, oldPacs.flip,
      transLogEntries, journalEntries, stocks, newStock, updatedArticle)

  private def filterIWS[A <: IWS](list: List[A], param: String): List[A] = list.filter(_.id == param)
  private def articleId2AccountId(articleId:String, articles:List[Article], accounts:List[Account]): (List[String], List[String]) =
    (filterIWS(articles,  articleId).flatMap(art=>filterIWS(accounts, art.stockAccount).map(_.id)),
     filterIWS(articles,  articleId).flatMap(art=>filterIWS(accounts, art.expenseAccount).map(_.id)))

  private[this] def buildPacsFromTransaction(model:Transaction, articles:List[Article], accounts:List[Account]) =
    model.lines.flatMap { line =>
      val (stockAccount, expenseAccount) = articleId2AccountId(line.article, articles, accounts)
    List(
      stockAccount.map( acc=>PeriodicAccountBalance.apply(
        PeriodicAccountBalance.createId(model.period, acc),
        acc,
        model.period,
        zeroAmount,
        zeroAmount,
        line.quantity.multiply(line.price),
        zeroAmount,
        line.currency,
        model.company,
        "",
        PeriodicAccountBalance.MODELID
      )),
      expenseAccount.map( acc=>
      PeriodicAccountBalance.apply(
        PeriodicAccountBalance.createId(model.period, acc),
        acc,
        model.period,
        zeroAmount,
        zeroAmount,
        zeroAmount,
        line.quantity.multiply(line.price),
        line.currency,
        model.company,
        "",
        PeriodicAccountBalance.MODELID
      )
    )
    ).flatten
  }

  private[this] def buildPacId(period: Int, accountId:(String,  String)): List[String] =
    List(PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))

  private[this] def buildPacId2(period: Int, accountId:(String,  String)): (String, String) =
    (PeriodicAccountBalance.createId(period, accountId._1), PeriodicAccountBalance.createId(period, accountId._2))
  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, Nothing, List[Stock]] = for{
      updatedStock <- updateOldStock(transactions, oldStocks).map(_.map(Stock.apply).flip).flatten
    }yield   updatedStock

  private def updateArticleAvgPrice(line: TransactionDetails, stocks:List[Stock], articles:List[Article]): ZIO[Any, Nothing, List[Article]] =
    articles.map ( article => {
    val purchasedValue = line.price.multiply(line.quantity)
    val wholeStock = groupByStockFirst(stocks.filter(st=>st.article == line.article))
    val wholeQuantityBefore = wholeStock.fold(zeroAmount)(_.quantity)
    val wholeValueBefore = wholeQuantityBefore.multiply(article.avgPrice)
    val wholeValueAfter = wholeValueBefore.add(purchasedValue)
    val wholeQuantityAfter = wholeQuantityBefore.add(line.quantity)
    val avgPriceAfter = if(wholeQuantityAfter.compareTo(zeroAmount)>0) wholeValueAfter.divide(wholeQuantityAfter, 2, RoundingMode.HALF_UP)  else line.price
    val avgPrice_ = wholeStock.fold(line.price)(_ =>avgPriceAfter)
      ZIO.succeed(article.copy(avgPrice = avgPrice_))
  }).flip
  private def updateAvgPrice(transactions: List[Transaction], stocks:List[Stock], articles:List[Article]): ZIO[Any, Nothing, List[Article]] =
    transactions.flatMap(tr => tr.lines.map(line => updateArticleAvgPrice( line,  stocks, articles.filter(_.id == line.article).distinct)))
    .flip.map(_.flatten)

  private def buildNewStock(transactions: List[Transaction], stocks:List[Stock]) = for {
    newRecords <-Stock.create(transactions).filterNot(stock=>stocks.map(_.id).contains(stock.id))
  }yield ZIO.succeed(newRecords)
  private def updateOldStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, Nothing, List[TStock]] = for {
    updatedStock <-groupByStock(Stock.create(transactions))
      .flatMap(ts=> oldStocks.filter(st=>st.id==ts.id)
      .map(st=>TStock.apply(st, ts.quantity))).flip
  }yield updatedStock

  private def updatePac(oldPacs: List[PeriodicAccountBalance], tpacs: List[TPeriodicAccountBalance]): ZIO[Any, Nothing, List[TPeriodicAccountBalance]] =
  for{
    newRecords<- groupById(oldPacs).map(TPeriodicAccountBalance.apply).flip
             _<- newRecords.map ( pac =>transfer(pac, tpacs)).flip
  }yield ( if(tpacs.nonEmpty) tpacs  else newRecords)

  private def transfer( pac:TPeriodicAccountBalance,  tpacs:List[TPeriodicAccountBalance]): ZIO[Any, Nothing, Option[Unit]] =
    tpacs.find(_.id ==  pac.id).map(pac_ => pac_.transfer(pac, pac_)).flip

  private def groupById(r: List[PeriodicAccountBalance]) =
  (r.groupBy(_.id) map { case (_, v) =>
      common.reduce(v, PeriodicAccountBalance.dummy)
    }).toList

  private def groupByStockFirst(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).headOption
  private def groupByStock(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).toList

  def findOrBuildPac(pacId: String, period_ : Int, pacList: List[PeriodicAccountBalance]): PeriodicAccountBalance =
    pacList.find(pac_ => pac_.id == pacId).getOrElse(PeriodicAccountBalance.dummy.copy(id = pacId, period = period_))

  private def makeJournal(models: List[Transaction],  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]],
                          articles:List[Article] ): ZIO[Any, Nothing, List[Journal]] = for {
      pacList <- tpacList.flip
      journal = models.flatMap(model =>
        if (model.modelid == TransactionModelId.GOORECEIVING.id) {
          model.lines.flatMap { line =>
            val accountId = articles.filter(_.id == line.article).map(art => (art.stockAccount, art.expenseAccount)).head
            val pacId = buildPacId2(model.getPeriod, (accountId._1, accountId._2))
            val pac = findOrBuildPac(pacId._1, model.getPeriod, pacList ++ pacListx)
            val poac = findOrBuildPac(pacId._2, model.getPeriod, pacList ++ pacListx)
            val jou1 = buildJournalEntries(model, line, pac, pacId._1, pacId._2, side = true)
            val jou2 = buildJournalEntries(model, line, poac, pacId._2, pacId._1, side = false)
            List(jou1, jou2)
          }
        } else List.empty[Journal])
    } yield journal

  private def buildJournalEntries(model: Transaction, line: TransactionDetails,
                                  pac: PeriodicAccountBalance, account: String, oaccount: String,side:Boolean) =
    Journal(
      -1,
      model.id,
      model.oid,
      account,
      oaccount,
      model.transdate,
      model.postingdate,
      model.enterdate,
      model.getPeriod,
      line.quantity.multiply(line.price),
      pac.idebit,
      pac.debit,
      pac.icredit,
      pac.credit,
      line.currency,
      side,
      line.text,
      model.month.toInt,
      model.year,
      model.company,
      model.modelid
    )

  private def buildTransactionLog(models: List[Transaction], stocks:List[Stock], newStock: List[Stock],
                          articles:List[Article] ): ZIO[Any, Nothing, List[TransactionLog]] = ZIO.succeed {
    val allStock = stocks++newStock
    models.flatMap(tr => tr.lines.map( line =>
      allStock.find(_.id == tr.store.concat(line.article).concat(tr.company).concat("")) // Find stock for article  in  store
      .flatMap(st=> articles.find(_.id == st.article).map( article =>                    // Find article for the line
     TransactionLog(0L, tr.id, tr.oid, tr.store, tr.costcenter, line.article, line.quantity // build TransactionLog
       , st.quantity, /*article.wholeStock*/ zeroAmount, article.quantityUnit , article.pprice , article.avgPrice
       , article.currency, line.duedate, line.text, tr.transdate, tr.postingdate, tr.enterdate, tr.period, tr.company, tr.modelid)
      ))).map(_.toList)).flatten
  }
}

object PostGoodreceivingImpl {
  val live: ZLayer[PacRepository with TransactionRepository with TransactionLogRepository with AccountRepository
    with ArticleRepository with StockRepository with PostTransactionRepository, RepositoryError, PostGoodreceiving] =
    ZLayer.fromFunction(new PostGoodreceivingImpl(_, _, _, _, _))
}


