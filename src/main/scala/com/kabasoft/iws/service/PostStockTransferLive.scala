package com.kabasoft.iws.service

import com.kabasoft.iws.domain.*
import com.kabasoft.iws.domain.AppError.RepositoryError
//import com.kabasoft.iws.domain.common.{*, given}
import com.kabasoft.iws.repository.*
import zio.*
import zio.prelude.FlipOps
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
      (sourceStocks, targetStocks) = Stock.create4Transfer(transactions)
      oldTargetStocks = targetStocks.filter(stock=>oldStocks.map(_.id).contains(stock.id))
      newStock = targetStocks.filterNot(stock=>oldStocks.map(_.id).contains(stock.id))
      articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
      articleIds = articleIdsx.distinct
      articles <- artRepo.getBy(articleIds, ModelId.ARTICLE.modelid, company.id)
      stocks <- updateStock(sourceStocks++oldTargetStocks, oldStocks, articles)
      transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
      updatedArticle =  updateArticle(transactions, articles)
      updatedTrans <- ZIO.foreach(transactions)(tr => updateTransactions(tr, articles))
      models = List.empty
      newPacs = List.empty
      oldPacs = ZIO.succeed(List.empty)
      journalEntries = List.empty

      nr <- repository4PostingTransaction.post(updatedTrans, models, newPacs, oldPacs, transLogEntries, journalEntries
        , stocks, newStock, updatedArticle)
    yield nr
    

  private def updateTransactions(tr: Transaction, articles: List[Article]): ZIO[Any, RepositoryError, Transaction] =
    for
      newLines <- ZIO.foreach(tr.lines)(line => updatePrice(articles, line))
    yield tr.copy(posted = true, lines = newLines)

  private def updatePrice(articles: List[Article], line: TransactionDetails): ZIO[Any, RepositoryError, TransactionDetails] =
    ZIO.getOrFailWith(RepositoryError(s"Article ${line.article} not found"))(
      articles.find(_.id == line.article)
    ).map(article => line.copy(price = article.avgPrice, transid = -1L))

  private def updateStock(stocks: List[Stock], oldStocks: List[Stock], articles: List[Article]): ZIO[Any, RepositoryError, List[Stock]] =
    for
      updatedStock <- updateOldStock(stocks, oldStocks, articles).map(_.map(Stock.apply).flip).flatten
    yield updatedStock

  
  private def updateArticle(transactions: List[Transaction], articles:List[Article]): List[Article] =
    transactions.flatMap(tr => tr.lines.flatMap(line => articles.filter(_.id == line.article).distinct)
     .map(_.copy(postingdate = Instant.now())))

  private def updateOldStock( stocks: List[Stock],
                              oldStocks: List[Stock],
                              articles: List[Article]
                            ): ZIO[Any, RepositoryError, List[TStock]] =
    ZIO.foreach(stocks) { stock =>
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
  

object PostStockTransferLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& ArticleRepository& 
    StockRepository& PostTransactionRepository, RepositoryError, PostStockTransfer] =
    ZLayer.fromFunction(new PostStockTransferLive(_, _, _))
