package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common.{given, _}
import com.kabasoft.iws.repository._
import zio._
import zio.prelude.FlipOps

import java.math.RoundingMode

final class PostGoodreceivingLive(pacRepo: PacRepository
                                  , accRepo: AccountRepository
                                  , artRepo: ArticleRepository
                                  , stockRepo: StockRepository
                                  , repository4PostingTransaction:PostTransactionRepository)
                                    extends PostGoodreceiving:

  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  = 
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
      _ <- ZIO.foreachDiscard(transactions.map(_.id))(
      id => ZIO.logDebug(s"Posting Goodreceiving transaction  with id ${id} of company ${transactions.head.company}"))
      stockIds = Stock.create(transactions).map(_.id).distinct
      oldStocks <- stockRepo.getBy(stockIds, Stock.MODELID, company.id)
      newStock <- buildNewStock(transactions, oldStocks).flip
      post <- postTransaction(transactions, company, newStock, oldStocks)
      nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8)
    yield nr

  private def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]):
  ZIO[Any, RepositoryError, (List[Transaction], List[PeriodicAccountBalance], ZIO[Any, Nothing, List[PeriodicAccountBalance]],
                             List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {

    accounts <- accRepo.all(Account.MODELID, company.id)
    articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
    articleIds = articleIdsx.distinct
    articles <- artRepo.getBy(articleIds, Article.MODELID, company.id)
    accountIds = articles.map(art => (art.account, company.purchasingClearingAcc))
    pacids = accountIds.flatMap(id => transactions.map(tr => buildPacId(tr.period, id))).flatten.distinct
    pacs <- pacRepo.getBy(pacids, Stock.MODELID, company.id).map(_.filterNot(_.id.equals(PeriodicAccountBalance.dummy.id)))
    allPacs = transactions.flatMap(tr => buildPacsFromTransaction(tr, articles, accounts, company.purchasingClearingAcc))
    newRecords = allPacs.filter(id=> pacs.map(_.id).contains(id))
    tpacs <- pacs.map(TPeriodicAccountBalance.apply).flip
    oldPacs <- updatePac(allPacs, tpacs).map(e => e.map(PeriodicAccountBalance.applyT))
    journalEntries <- makeJournal(transactions, newRecords.toList, oldPacs, articles)
    stocks <- updateStock(transactions, oldStocks)
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
    updatedArticle <- updateAvgPrice(transactions, stocks, articles)
  } yield (transactions, newRecords.toList, oldPacs.flip,
      transLogEntries, journalEntries, stocks, newStock, updatedArticle)

  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, Nothing, List[Stock]] = 
    for 
      updatedStock <- updateOldStock(transactions, oldStocks).map(_.map(Stock.apply).flip).flatten
    yield   updatedStock

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
      ZIO.succeed(article.copy(avgPrice = avgPrice_, pprice = line.price))
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

  private def groupByStockFirst(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).headOption
  private def groupByStock(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).toList

  private def makeJournal(models: List[Transaction],  pacListx: List[PeriodicAccountBalance], tpacList: List[UIO[PeriodicAccountBalance]],
                          articles:List[Article] ): ZIO[Any, Nothing, List[Journal]] =
    if (articles.isEmpty) throw IllegalStateException(" Error: a  goodreceiving transaction without article may not be posted!!!")
    for {
      pacList <- tpacList.flip
      journal = models.flatMap(model =>
          model.lines.flatMap { line =>
            val accountId = articles.filter(_.id == line.article).map(art => (art.account, art.oaccount)).head
            val pacId = buildPacId2(model.getPeriod, (accountId._1, accountId._2))
            val pac = findOrBuildPac(pacId._1, model.getPeriod, pacList ++ pacListx)
            val poac = findOrBuildPac(pacId._2, model.getPeriod, pacList ++ pacListx)
            val jou1 = buildJournalEntries(model, line, pac, pacId._1, pacId._2, side = true)
            val jou2 = buildJournalEntries(model, line, poac, pacId._2, pacId._1, side = false)
            List(jou1, jou2)
          })
    } yield journal


object PostGoodreceivingLive:
  val live: ZLayer[PacRepository& TransactionRepository& TransactionLogRepository& AccountRepository&
     ArticleRepository& StockRepository& PostTransactionRepository, RepositoryError, PostGoodreceiving] =
    ZLayer.fromFunction(new PostGoodreceivingLive(_, _, _, _, _))
