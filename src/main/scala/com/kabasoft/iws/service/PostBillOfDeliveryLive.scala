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

//  private def articleId2Account(articleId: String, articles: List[Article], accounts: List[Account], flag:Boolean): Account = {
//    filterIWS(articles, articleId).flatMap(art => {
//      if (flag) filterIWS(accounts, art.account) else  filterIWS(accounts, art.oaccount)
//    }).headOption.getOrElse(Account.dummy)
//  }
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  = {
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty ) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for {
      _ <- ZIO.foreachDiscard(transactions.map(_.id))(
        id => ZIO.logDebug(s"Posting bill of delivery  transaction  with id ${id} of company ${transactions.head.company}"))
      stockIds = Stock.create(transactions).map(_.id).distinct
      oldStocks <- stockRepo.getBy(stockIds, Stock.MODELID, company.id)
        _       <- ZIO.logInfo(s"Stock  from  bill of delivery  transaction  ${oldStocks}")
        _       <-  ZIO.when(oldStocks.isEmpty)(ZIO.fail( RepositoryError(s"No Stock found for ${stockIds}")))
      post     <- postTransaction(transactions, company, Nil, oldStocks)
      //nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6)
      nr <- repository4PostingTransaction.post(post._1, post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
    } yield nr
  }
  
  private def postTransaction(transactions: List[Transaction], company: Company, newStock:List[Stock], oldStocks:List[Stock]):
      ZIO[Any, RepositoryError, (List[Transaction], List[FinancialsTransaction], List[PeriodicAccountBalance]
    , ZIO[Any, Nothing, List[PeriodicAccountBalance]], List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {

    accounts <- accRepo.all(Account.MODELID, company.id)
    articles <- artRepo.getBy(transactions.flatMap(m => m.lines.map(_.article)).distinct, Article.MODELID, company.id)
    //  uodate stock transactionaly using stm
    stocks <- updateStock(transactions, oldStocks)
    _<-ZIO.logInfo(s"Stocks   from  bill of delivery  transaction with  of company ${stocks}")
    customers <- customerRepo.all(Customer.MODELID, company.id)
    vatIds = transactions.flatMap(_.lines.map(_.vatCode)).distinct
    vats <-  vatRepo.getBy(vatIds, Vat.MODEL_ID, company.id)
    // build transaction's log entries
    transLogEntries <- buildTransactionLog(transactions, stocks, newStock, articles)
    // build financials transaction 
    newFtr = transactions.map(buildFinancials(_,  articles, accounts, customers, TransactionModelId.RECEIVABLES.id))
    (transaction:List[Transaction], financials:List[FinancialsTransaction]) = newFtr.unzip
    result <- postFinancials(financials, financialsService)
    models = result.map(_._1)
    newPacs = result.map(_._2).flatten
    oldPacs = result.map(_._3).flip.map(_.flatten)
    journalEntries = result.map(_._4).flatten
    _<-ZIO.logInfo(s"result2   from  bill of delivery  transaction with  of company ${result}")
    _<-ZIO.logInfo(s"new Pacs   from  bill of delivery  transaction with  of company ${newPacs}")
    _<-ZIO.logInfo(s"Oldoacs   from  bill of delivery  transaction with  of company ${oldPacs}")
    _<-ZIO.logInfo(s"Transaction log entries ${transLogEntries}")
  } yield ( transaction, models, newPacs, oldPacs, transLogEntries, journalEntries, stocks, newStock, Nil)

  def buildFinancials(model: Transaction, articles: List[Article], accounts: List[Account], suppliers: List[BusinessPartner]
                       ,  modelid: Int): (Transaction, FinancialsTransaction) = {
    // Fetch the supplier using its id from the transaction (account field)
    val partnerAccountId = suppliers.filter(_.id == model.account)
      .flatMap(s => accounts.filter(_.id == s.account))
      .headOption.getOrElse(Account.dummy).id
    val currency = model.lines.headOption.getOrElse(TransactionDetails.dummy).currency
    // build details for net amount
    val netDetails: List[FinancialsTransactionDetails] = model.lines.map { line =>
        val article = articles.find(_.id == line.article).getOrElse(Article.dummy)
        val account = articleId2Account(line.article, articles, accounts, false)
        val oaccount = articleId2Account(line.article, articles, accounts, true)
        FinancialsTransactionDetails(-1, 0, account.id, side = true, oaccount.id, line.quantity.multiply(article.avgPrice), Instant.now()
          , model.text, currency, model.company, account.name, oaccount.name)
    }.groupBy(line => (line.account, line.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)
    }.toList

    val details: List[FinancialsTransactionDetails] = netDetails.filterNot(_.account == FinancialsTransactionDetails.dummy.account)
      .groupBy(d => (d.account, d.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy) }.toList
    val financialsTransaction = FinancialsTransaction(-1, model.id, 0, model.store, partnerAccountId, model.transdate
      , Instant.now(), Instant.now(), model.period, posted = false, modelid, model.company, model.text, -1, -1, details)
    (model.copy(posted = true), financialsTransaction.copy(posted = true))
  }
  
  private def updateStock(transactions: List[Transaction], oldStocks:List[Stock]): ZIO[Any, RepositoryError, List[Stock]] = for{
      updatedStock <- updateOldStock(Stock.create(transactions).map(stock =>stock.copy(quantity = stock.quantity.negate())), oldStocks)
        .map(_.map(Stock.apply).flip).flatten
    }yield   updatedStock

  private def updateOldStock(stocksNew:List[Stock], oldStocks:List[Stock]): ZIO[Any, RepositoryError, List[TStock]]=
    stocksNew
    .flatMap(ts=> oldStocks.filter(_.id==ts.id)
      .map(st=>TStock.apply(st, ts.quantity))).flip

object PostBillOfDeliveryLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& AccountRepository& VatRepository&
     ArticleRepository& CustomerRepository &StockRepository& PostTransactionRepository&FinancialsService, RepositoryError, PostBillOfDelivery] =
    ZLayer.fromFunction(new PostBillOfDeliveryLive(_, _, _, _, _, _, _))
