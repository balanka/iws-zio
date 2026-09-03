package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
import scala.collection.immutable.{List, Nil}
import zio.prelude.FlipOps
import java.time.Instant
import zio.IsSubtypeOfError.impl

case class Helper (financials:FinancialsTransaction, newPacs:List[PeriodicAccountBalance]
                   , oldPacs:UIO[List[PeriodicAccountBalance]], journals:List[Journal])
final class PostBillOfDeliveryLive( accRepo: AccountRepository
                                   , artRepo: ArticleRepository
                                   , vatRepo: VatRepository
                                   , customerRepo: CustomerRepository
                                   , stockRepo: StockRepository
                                   , repository4PostingTransaction:PostTransactionRepository
                                   , financialsService:FinancialsService
                                  ) extends PostBillOfDelivery:

  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  = {
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty ) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for {
      _ <- ZIO.foreachDiscard(transactions.map(_.id))(
        id => ZIO.logDebug(s"Posting bill of delivery  transaction  with id ${id} of company ${transactions.head.company}"))
      articleIdsx = transactions.flatMap(m => m.lines.map(_.article))
      articleIds = articleIdsx.distinct
      articles <- artRepo.getBy(articleIds, ModelId.ARTICLE.modelid, company.id)
      //articles  <-  artRepo.all((ModelId.ARTICLE.modelid, company.id))
      stockIds = Stock.create(transactions, articles).map(_.id).distinct
      oldStocks <- stockRepo.getBy(stockIds, ModelId.STOCK.modelid, company.id)
        _       <- ZIO.logInfo(s"Stock  from  bill of delivery  transaction  ${oldStocks}")
        _       <-  ZIO.when(oldStocks.isEmpty)(ZIO.fail( RepositoryError(s"No Stock found for ${stockIds}")))
      post     <- postTransaction(transactions, company, Nil, oldStocks)
      nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
    } yield nr
  }
  
  private def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]):
      ZIO[Any, RepositoryError, (List[Transaction], List[FinancialsTransaction], List[PeriodicAccountBalance]
    , ZIO[Any, Nothing, List[PeriodicAccountBalance]], List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {

    accounts <- accRepo.all(ModelId.ACCOUNT.modelid, company.id)
    vats <- vatRepo.all(ModelId.VAT.modelid, company.id)
    articles <- artRepo.getBy(transactions.flatMap(m => m.lines.map(_.article)).distinct, ModelId.ARTICLE.modelid, company.id)
    sourceStocks = Stock.create(transactions, articles).map(stock=>stock.copy(quantity = stock.quantity.negate()))
    //  uodate stock transactionaly using stm
    //stocks <- updateStock(transactions, articleAll, oldStocks)
    stocks <- updateStock_(sourceStocks, articles, oldStocks)
    _<-ZIO.logInfo(s"Stocks   from  bill of delivery  transaction with  of company ${stocks}")
    customers <- customerRepo.all(ModelId.CUSTOMER.modelid, company.id)
    // build transaction's log entries
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
    updatedArticle = articles.map(_.copy(postingdate = Instant.now()))
    // build financials transaction
    newFtr = transactions.map(buildConsumption(_,  articles, accounts, ModelId.RECEIVABLES.modelid))
    newFtr1 = transactions.map(buildTransaction(_, articles, accounts, customers, vats, company.salesClearingAcc, ModelId.RECEIVABLES.modelid))
    tupleOfLists <- ZIO.collectAll(newFtr).map(_.unzip)   // <- binds the effect
    tupleOfLists1 <- ZIO.collectAll(newFtr1).map(_.unzip)
    (transactionsx, financials) = tupleOfLists
    (transactionsx1, financials1) = tupleOfLists1
    result <- postFinancials(financials++financials1, financialsService)
    models = result.map(_._1)
    newPacs = result.flatMap(_._2)
    oldPacs = result.map(_._3).flip.map(_.flatten)
    journalEntries = result.flatMap(_._4)
    _<-ZIO.logInfo(s"result2   from  bill of delivery  transaction with  of company ${result}")
    _<-ZIO.logInfo(s"new Pacs   from  bill of delivery  transaction with  of company ${newPacs}")
    _<-ZIO.logInfo(s"Oldoacs   from  bill of delivery  transaction with  of company ${oldPacs}")
    _<-ZIO.logInfo(s"Transaction log entries ${transLogEntries}")
  } yield ( transactionsx++transactionsx1, models, newPacs, oldPacs, transLogEntries, journalEntries, stocks, newStock, updatedArticle)

  private def buildConsumption(model: Transaction, articles: List[Article], accounts: List[Account]
                               , modelid: Int): ZIO[Any, RepositoryError, (Transaction, FinancialsTransaction)] = {
    var currency = model.lines.headOption.getOrElse(TransactionDetails.dummy).currency
    // build details for net amount
    val netDetails: List[FinancialsTransactionDetails] = model.lines.map { line =>
      val article = articles.find(_.id == line.article).getOrElse(Article.dummy)
      val account = filterIWS(accounts, article.oaccount).headOption.getOrElse(Account.dummy)
      val oaccount = filterIWS(accounts, article.account).headOption.getOrElse(Account.dummy)
      currency = line.currency
      //partnerAccountId = account.id
      FinancialsTransactionDetails(-1, 0, account.id, side = true, oaccount.id, line.quantity.multiply(article.avgPrice), Instant.now()
        , model.text, currency, model.company, account.name, oaccount.name, modelid)
    }.groupBy(line => (line.account, line.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)
    }.toList
    val head = netDetails.headOption.getOrElse(FinancialsTransactionDetails.dummy)
    val details: List[FinancialsTransactionDetails] = netDetails.filterNot(_.account == FinancialsTransactionDetails.dummy.account)
      .groupBy(d => (d.account, d.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy) }.toList
    val financialsTransaction = FinancialsTransaction(-1, model.id.toString, model.contact, head.account, head.oaccount, model.transdate
      , Instant.now(), Instant.now(), model.period, posted = false, modelid, model.company, model.text, model.footText, -1, details)
    ZIO.succeed((model.copy(posted = true), financialsTransaction.copy(posted = true)))
  }

object PostBillOfDeliveryLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& AccountRepository& ArticleRepository& VatRepository&
      CustomerRepository &StockRepository& PostTransactionRepository&FinancialsService, RepositoryError, PostBillOfDelivery] =
    ZLayer.fromFunction(new PostBillOfDeliveryLive(_, _, _, _, _, _, _))
