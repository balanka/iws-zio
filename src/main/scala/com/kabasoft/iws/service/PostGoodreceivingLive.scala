package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common.{given, _}
import com.kabasoft.iws.repository._
import zio._
import zio.prelude.FlipOps
import java.math.RoundingMode

final class PostGoodreceivingLive(accRepo: AccountRepository
                                  , supplierRepo: SupplierRepository
                                  , vatRepo: VatRepository
                                  , artRepo: ArticleRepository
                                  , stockRepo: StockRepository
                                  , financialsService:FinancialsService
                                  , repository4PostingTransaction:PostTransactionRepository)
                                    extends PostGoodreceiving:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  = 
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
      //_ <- ZIO.foreachDiscard(transactions.map(_.id))(
      //id => ZIO.logInfo(s"Posting Goodreceiving transaction  with id ${id} of company ${transactions.head.company}"))
      
      articles  <-  artRepo.all((ModelId.ARTICLE.modelid, company.id))
      stockIds = Stock.create(transactions, articles).map(_.id).distinct
      oldStocks <- stockRepo.getBy(stockIds, ModelId.STOCK.modelid, company.id)
      newStock <- buildNewStock(transactions, articles, oldStocks).flip
      post <- postTransaction(transactions, company, newStock, oldStocks)
      nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
    yield nr
    

  private def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]):
  ZIO[Any, RepositoryError, (List[Transaction], List[FinancialsTransaction], List[PeriodicAccountBalance]
    , ZIO[Any, Nothing, List[PeriodicAccountBalance]], List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {
    accounts <- accRepo.all(ModelId.ACCOUNT.modelid, company.id)
    articleAll <- artRepo.all(ModelId.ARTICLE.modelid, company.id)
    articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
    articleIds = articleIdsx.distinct
    articles <- artRepo.getBy(articleIds, ModelId.ARTICLE.modelid, company.id)
    suppliers <- supplierRepo.all(ModelId.SUPPLIER.modelid, company.id)
    vatIds = transactions.flatMap(_.lines.map(_.vatCode)).distinct
    vats <-  vatRepo.getBy(vatIds, ModelId.VAT.modelid, company.id)
    stocks <- updateStock(transactions, articleAll, oldStocks)
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
    _<- ZIO.logInfo(s" transLogEntries ${transLogEntries}")
    updatedArticle <- updateAvgPrice(transactions, stocks, articles)
    _<- ZIO.logInfo(s" updatedArticle ${updatedArticle}")
    // build financials transaction
    newFtr = transactions.map(buildTransaction(_,  articles, accounts, suppliers, vats, company.purchasingClearingAcc
      , ModelId.PAYABLES.modelid))
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
    _<-ZIO.logInfo(s"stocks ${stocks}")
    _<-ZIO.logInfo(s"newStock ${newStock}")
    _<-ZIO.logInfo(s"Transaction log entries ${transLogEntries}")

  } yield ( transactionsx, models, newPacs, oldPacs, transLogEntries, journalEntries, stocks, newStock, updatedArticle)

  private def updateArticleAvgPrice(line: TransactionDetails, stocks:List[Stock], articles:List[Article]): ZIO[Any, Nothing, List[Article]] =
    articles.map ( article => {
    val purchasedValue = line.price.multiply(line.quantity)
    val wholeStock = groupByStock(stocks.filter(st=>st.article == line.article)).headOption
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


object PostGoodreceivingLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& AccountRepository& VatRepository&
      SupplierRepository& ArticleRepository& StockRepository& PostTransactionRepository&FinancialsService, RepositoryError, PostGoodreceiving] =
    ZLayer.fromFunction(new PostGoodreceivingLive(_, _, _, _, _, _, _))
