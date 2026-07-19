package com.kabasoft.iws.service

import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository._
import zio._
import zio.prelude.FlipOps
import java.time.Instant

trait PostStocktakeOrConsumption extends PostLogisticalTransaction:
   def postAll(transactions: List[Transaction], company:Company, stockRepo:StockRepository
  , accRepo:AccountRepository, artRepo: ArticleRepository , financialsService:FinancialsService
               , repository4PostingTransaction:PostTransactionRepository): ZIO[Any, RepositoryError, Int]  =
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
      _ <- ZIO.foreachDiscard(transactions.map(_.id))(
      id => ZIO.logDebug(s"Posting the stock take transaction  with id $id of company ${transactions.head.company}"))
      articles  <-  artRepo.all((ModelId.ARTICLE.modelid, company.id))
      stockIds = Stock.create(transactions, articles).map(_.id).distinct
      oldStocks <- stockRepo.getBy(stockIds, ModelId.STOCK.modelid, company.id)
      newStock <- buildNewStock(transactions, articles, oldStocks ).flip
      post <- postTransaction(transactions, company, newStock, oldStocks, accRepo, artRepo, financialsService)
      nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
    yield nr
    

   private def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]
                , accRepo:AccountRepository, artRepo: ArticleRepository
                , financialsService:FinancialsService):
      ZIO[Any, RepositoryError, (List[Transaction], List[FinancialsTransaction], List[PeriodicAccountBalance]
      , ZIO[Any, Nothing, List[PeriodicAccountBalance]], List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {
      accounts <- accRepo.all(ModelId.ACCOUNT.modelid, company.id)
      articleAll <- artRepo.all(ModelId.ARTICLE.modelid, company.id)
      articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
      articleIds = articleIdsx.distinct
      articles <- artRepo.getBy(articleIds, ModelId.ARTICLE.modelid, company.id)
      suppliers = List.empty
      vats = List.empty
    
      stocks <- updateStock(transactions, articleAll, oldStocks)
      transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
      updatedArticle = articles.map(_.copy(postingdate = Instant.now()))
      newFtr = transactions.map(tr =>buildTransaction(tr, articles, accounts, suppliers, vats,
      tr.account, ModelId.STOCK_TAKE.modelid))
      tupleOfLists <- ZIO.collectAll(newFtr).map(_.unzip)   // <- binds the effect
      (transactionsx, financials) = tupleOfLists

      _<- ZIO.logInfo(s"New Transactions $transactionsx")
      _<- ZIO.logInfo(s"New Financials $financials")
      result <- postFinancials(financials, financialsService)
      models = result.map(_._1)
      newPacs = result.flatMap(_._2)
      oldPacs = result.map(_._3).flip.map(_.flatten)
      journalEntries = result.flatMap(_._4)
      _<-ZIO.logInfo(s"result2   from  bill of delivery  transaction with  of company $result")
      _<-ZIO.logInfo(s"new Pacs   from  bill of delivery  transaction with  of company $newPacs")
      _<-ZIO.logInfo(s"Oldoacs   from  bill of delivery  transaction with  of company $oldPacs}")
      _<-ZIO.logInfo(s"Transaction log entries $transLogEntries")
  } yield ( transactionsx, models, newPacs, oldPacs, transLogEntries, journalEntries, stocks, newStock, updatedArticle)


