package com.kabasoft.iws.service

import com.kabasoft.iws.domain.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.common.{*, given}
import com.kabasoft.iws.repository.*
import zio.*
import zio.prelude.FlipOps

import java.math.RoundingMode
import java.time.Instant

final class PostStockTransferLive( artRepo: ArticleRepository
                                  , stockRepo: StockRepository
                                  , repository4PostingTransaction:PostTransactionRepository)
                                    extends PostStockTransfer:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  = 
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
      _ <- ZIO.foreachDiscard(transactions.map(_.id))(
      id => ZIO.logDebug(s"Posting the stock transfer transaction  with id ${id} of company ${transactions.head.company}"))
      stockIds = Stock.create(transactions).map(_.id).distinct
      oldStocks <- stockRepo.getBy(stockIds, ModelId.STOCK.modelid, company.id)
      newStock <- buildNewStock(transactions, oldStocks).flip
      post <- postTransaction(transactions, company, newStock, oldStocks)
      nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
    yield nr
    

  private def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]):
  ZIO[Any, RepositoryError, (List[Transaction], List[FinancialsTransaction], List[PeriodicAccountBalance]
    , ZIO[Any, Nothing, List[PeriodicAccountBalance]], List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {
    articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
    articleIds = articleIdsx.distinct
    articles <- artRepo.getBy(articleIds, ModelId.ARTICLE.modelid, company.id)
    stocks <- updateStock(transactions, oldStocks, articles)
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
    updatedArticle =  updateArticle(transactions, stocks, articles)
    updatedTrans <- ZIO.foreach(transactions)(tr => updateTransactions(tr, articles))
//    _<- ZIO.logInfo(s"New Transactions ${transaction}")
//    _<- ZIO.logInfo(s"New Financials ${financials}")
//    result <- postFinancials(financials, financialsService)
    models = List.empty
    newPacs = List.empty
    oldPacs = ZIO.succeed(List.empty)
    journalEntries = List.empty
//    _<-ZIO.logInfo(s"result2   from  bill of delivery  transaction with  of company ${result}")
//    _<-ZIO.logInfo(s"new Pacs   from  bill of delivery  transaction with  of company ${newPacs}")
//    _<-ZIO.logInfo(s"Oldoacs   from  bill of delivery  transaction with  of company ${oldPacs}")
    _<-ZIO.logInfo(s"Transaction log entries ${transLogEntries}")
    //journalEntries <- makeJournal(transactions, newRecords, oldPacs, articles, company.purchasingClearingAcc)

  } yield ( updatedTrans, models, newPacs, oldPacs, transLogEntries, journalEntries, stocks, newStock, updatedArticle)
  
  def updateTransactions(tr: Transaction, articles: List[Article]): ZIO[Any, RepositoryError, Transaction] =
    for
      newLines <- ZIO.foreach(tr.lines)(line => updatePrice(articles, line))
    yield tr.copy(posted = true, lines = newLines)

  private def updatePrice(articles: List[Article], line: TransactionDetails): ZIO[Any, RepositoryError, TransactionDetails] =
    ZIO.getOrFailWith(RepositoryError(s"Article ${line.article} not found"))(
      articles.find(_.id == line.article)
    ).map(article => line.copy(price = article.avgPrice))
  
  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock], articles:List[Article]): ZIO[Any, RepositoryError, List[Stock]] =
    for 
      updatedStock <- updateOldStock(transactions, oldStocks, articles).map(_.map(Stock.apply).flip).flatten
    yield   updatedStock

  private def updateArticle(line: TransactionDetails, stocks:List[Stock], articles:List[Article]): ZIO[Any, Nothing, List[Article]] =
    articles.map ( article => {
      ZIO.succeed(article.copy(postingdate = Instant.now()))
  }).flip
  
  private def updateArticle(transactions: List[Transaction], stocks:List[Stock], articles:List[Article]): List[Article] =
    transactions.flatMap(tr => tr.lines.flatMap(line => articles.filter(_.id == line.article).distinct)
     .map(_.copy(postingdate = Instant.now())))


  private def buildNewStock(transactions: List[Transaction], stocks:List[Stock]) = for {
    newRecords <-Stock.create(transactions).filterNot(stock=>stocks.map(_.id).contains(stock.id))
  }yield ZIO.succeed(newRecords)

  private def updateOldStock( transactions: List[Transaction],
                              oldStocks: List[Stock],
                              articles: List[Article]
                            ): ZIO[Any, RepositoryError, List[TStock]] =
    ZIO.foreach(Stock.create(transactions)) { stock =>
      for {
        article <- ZIO.getOrFailWith(RepositoryError(s"Article ${stock.article} not found"))(
          articles.find(_.id == stock.article)
        )
        oldStock <- ZIO.getOrFailWith(RepositoryError(s"Old stock with id ${stock.id} not found"))(
          oldStocks.find(_.id == stock.id)
        )
        newQuantity = oldStock.quantity.add(stock.quantity)
        updatedStock = oldStock.copy(quantity = newQuantity, price = article.avgPrice)
        tstock <- TStock.fromStockAndQuantity(updatedStock, stock.quantity)
      } yield tstock
    }
  
  private def groupByStockFirst(r: List[Stock]) =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).headOption

  private def groupByStock(r: List[Stock], articles: List[Article]): List[Stock] =
    (r.groupBy(_.article) map { case (_, v) =>
      common.reduce(v, Stock.dummy)
    }).filterNot(_.article == Stock.dummy.article).toList

  private def groupByStockWithPrice(r: List[Stock], articles: List[Article]): List[Stock] =
    r.groupBy(_.article)
      .map { case (articleId, stocks) =>
        val reduced = common.reduce(stocks, Stock.dummy)
        articles.find(_.id == articleId).fold(reduced) { article =>
          reduced.copy(price = article.avgPrice)
        }
      }
      .filterNot(_.article == Stock.dummy.article)
      .toList

object PostStockTransferLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& ArticleRepository& 
    StockRepository& PostTransactionRepository, RepositoryError, PostStockTransfer] =
    ZLayer.fromFunction(new PostStockTransferLive(_, _, _))
