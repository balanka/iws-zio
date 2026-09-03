package com.kabasoft.iws.service

import com.kabasoft.iws.domain.*
import com.kabasoft.iws.domain.AppError.RepositoryError
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
      id => ZIO.logInfo(s"Posting the stock transfer transaction  with id ${id} of company ${transactions.head.company}"))
      articles_ <- artRepo.all(ModelId.ARTICLE.modelid, company.id)
      (sourceStocks, targetStocks) = Stock.create4Transfer(transactions, articles_ )
      _ <- ZIO.logInfo(s" sourceStocks ${sourceStocks}")
      _ <- ZIO.logInfo(s" targetStocks ${targetStocks}")
      sourceStockIds = (sourceStocks).map(_.id).distinct
      targetStockIds = (targetStocks).map(_.id).distinct
      //_ <- ZIO.logInfo(s" stockIds ${stockIds}")
      //stockIds = Stock.create(transactions, articles_ ).map(_.id).distinct
      oldSourceStocks <- stockRepo.getBy(sourceStockIds, ModelId.STOCK.modelid, company.id)
      oldTargetStocks <- stockRepo.getBy(targetStockIds, ModelId.STOCK.modelid, company.id)
      targetStocks2Update = targetStocks.filter(stock=>oldTargetStocks.map(_.id).contains(stock.id))
      _ <- ZIO.logInfo(s" oldSourceStocks ${oldSourceStocks}")
      _ <- ZIO.logInfo(s" oldTargetStocks ${oldTargetStocks}")
      _ <- ZIO.logInfo(s" targetStocks2Update ${targetStocks2Update}")
      //(sourceStocks, targetStocks) = Stock.create4Transfer(transactions, articles_ )
      newTargetStocks = targetStocks.filterNot(stock=>oldTargetStocks.map(_.id).contains(stock.id))
      _ <- ZIO.logInfo(s" newTargetStocks ${newTargetStocks}")
      newStock = newTargetStocks //targetStocks.filterNot(stock=>oldStocks.map(_.id).contains(stock.id))
      //_ <- ZIO.logInfo(s" newStock ${newStock}")
      articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
      articleIds = articleIdsx.distinct
      articles <- artRepo.getBy(articleIds, ModelId.ARTICLE.modelid, company.id)
      _ <- ZIO.logInfo(s" newStock ${newStock}")
      //stocks <- updateStock_(sourceStocks++oldTargetStocks, articles, oldSourceStocks++oldTargetStocks)
      //updatedSourceStocks <- updateStock(transactions, articles, oldSourceStocks)
      updatedSourceStocks <- updateStock_(sourceStocks, articles, oldSourceStocks)
      _ <- ZIO.logInfo(s" updatedSourceStocks ${updatedSourceStocks}")
      updatedTargetStocks <- updateStock_(targetStocks2Update, articles, oldTargetStocks)
      _ <- ZIO.logInfo(s" updatedTargetStocks ${updatedTargetStocks}")
      stocks = updatedSourceStocks++updatedTargetStocks
      _ <- ZIO.logInfo(s" all stocks ${stocks}")
      transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
      updatedArticle =  updateArticle(transactions, articles)
      updatedTrans <- ZIO.foreach(transactions)(tr => updateTransactions(tr, articles))
      _ <- ZIO.logInfo(s" Updated transactions ${updatedTrans}")
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

//  private def updateStock_(stocks: List[Stock], articles: List[Article], oldStocks: List[Stock]): ZIO[Any, RepositoryError, List[Stock]] =
//    for
//      updatedStock <- updateOldStock_(stocks, oldStocks, articles).map(_.map(Stock.apply).flip).flatten
//      _ <- ZIO.logInfo(s" updatedStock ${updatedStock}")
//    yield updatedStock

  
  private def updateArticle(transactions: List[Transaction], articles:List[Article]): List[Article] =
    transactions.flatMap(tr => tr.lines.flatMap(line => articles.filter(_.id == line.article).distinct)
     .map(_.copy(postingdate = Instant.now())))

//  private def updateOldStock_( stocks: List[Stock], oldStocks: List[Stock], articles: List[Article]
//                            ): ZIO[Any, RepositoryError, List[TStock]] =
//    ZIO.foreach(stocks) { stock =>
//      for {
//        article <- ZIO.getOrFailWith(RepositoryError(s"Article ${stock.article} not found"))(
//          articles.find(_.id == stock.article)
//        )
//        _ <- ZIO.logInfo(s" article $article stock $stock  oldStocks=>> $oldStocks")
//        oldStock <- ZIO.getOrFailWith(RepositoryError(s"Old stock with id ${stock.id} not found"))(
//          oldStocks.find(_.id == stock.id)
//        )
//        _ <- ZIO.logInfo(s" stock->$stock  oldStock-> ${oldStock}")
//        tstock <- TStock.fromStockAndQuantity(oldStock, stock.quantity, articles).tapError(e => ZIO.logError(s"Stock error: $e"))
//        _ <- ZIO.logInfo(s" tstock ${tstock}")
//      } yield tstock
//    }
  

object PostStockTransferLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& ArticleRepository& 
    StockRepository& PostTransactionRepository, RepositoryError, PostStockTransfer] =
    ZLayer.fromFunction(new PostStockTransferLive(_, _, _))
