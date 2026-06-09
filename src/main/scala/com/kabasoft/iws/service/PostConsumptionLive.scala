package com.kabasoft.iws.service

import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{_, given}
import com.kabasoft.iws.repository._
import zio._
import zio.prelude.FlipOps

import java.math.RoundingMode

final class PostConsumptionLive(accRepo: AccountRepository
                                  , artRepo: ArticleRepository
                                  , stockRepo: StockRepository
                                  , financialsService:FinancialsService
                                  , repository4PostingTransaction:PostTransactionRepository)
                                    extends PostConsumption:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  = 
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
      _ <- ZIO.foreachDiscard(transactions.map(_.id))(
      id => ZIO.logDebug(s"Posting the consumption transaction  with id ${id} of company ${transactions.head.company}"))
      stockIds = Stock.create(transactions).map(_.id).distinct
      oldStocks <- stockRepo.getBy(stockIds, ModelId.STOCK.modelid, company.id)
      newStock <- buildNewStock(transactions, oldStocks).flip
      post <- postTransaction(transactions, company, newStock, oldStocks)
      nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
    yield nr
    

  private def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]):
  ZIO[Any, RepositoryError, (List[Transaction], List[FinancialsTransaction], List[PeriodicAccountBalance]
    , ZIO[Any, Nothing, List[PeriodicAccountBalance]], List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {
    accounts <- accRepo.all(ModelId.ACCOUNT.modelid, company.id)
    articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
    articleIds = articleIdsx.distinct
    articles <- artRepo.getBy(articleIds, ModelId.ARTICLE.modelid, company.id)
    stocks <- updateStock(transactions, oldStocks)
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
    updatedArticle <- updateAvgPrice(transactions, stocks, articles)
    // build financials transaction
    newFtr = transactions.map(buildTransaction(_,  articles, accounts, List.empty[BusinessPartner], List.empty[Vat], company.purchasingClearingAcc
      , TransactionModelId.PAYABLES.modelid))
    tupleOfLists <- ZIO.collectAll(newFtr).map(_.unzip)   // <- binds the effect
    (transactionsx, financials) = tupleOfLists
    _<- ZIO.logInfo(s"New Transactions ${transactionsx}")
    _<- ZIO.logInfo(s"New Financials ${financials}")
    result <- postFinancials(financials, financialsService)
    models = result.map(_._1)
    newPacs = result.flatMap(_._2)
    oldPacs = result.map(_._3).flip.map(_.flatten)
    journalEntries = result.flatMap(_._4)
    _<-ZIO.logInfo(s"result2   from  bill of delivery  transaction with  of company ${result}")
    _<-ZIO.logInfo(s"new Pacs   from  bill of delivery  transaction with  of company ${newPacs}")
    _<-ZIO.logInfo(s"Oldoacs   from  bill of delivery  transaction with  of company ${oldPacs}")
    _<-ZIO.logInfo(s"Transaction log entries ${transLogEntries}")
    //journalEntries <- makeJournal(transactions, newRecords, oldPacs, articles, company.purchasingClearingAcc)

  } yield ( transactionsx, models, newPacs, oldPacs, transLogEntries, journalEntries, stocks, newStock, updatedArticle)

  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, RepositoryError, List[Stock]] =
    for
      updatedTstock <- updateOldStock(transactions, oldStocks) //<-build stock from transaction, filter and retains old stock and update  them using stm
      updatedStock <-  updatedTstock.map(tstock =>Stock.apply(tstock)).flip //<-convert back from Tstock to  stock and flatten ( we had List[List[STock]]
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
  
  private def updateOldStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, RepositoryError, List[TStock]] = for {
    updatedStock <-groupByStock(Stock.create(transactions)).map(stock=>stock.copy(quantity = stock.quantity.negate()))         //<-build stock from transaction
      .flatMap(ts=> oldStocks.filter(st=>st.id==ts.id)              //<-Filter to retain only existing stock which should be updated
      .map(st=>TStock.fromStockAndQuantity(st, ts.quantity))).flip //<-convert them to TStock before updating and update transactionally
  }yield updatedStock

  private def groupByStockFirst(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).headOption
    
  private def groupByStock(r: List[Stock]): List[Stock] =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).toList

object PostConsumptionLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& AccountRepository&
      ArticleRepository& StockRepository& PostTransactionRepository&FinancialsService, RepositoryError, PostConsumption] =
    ZLayer.fromFunction(new PostConsumptionLive(_, _, _, _, _))
