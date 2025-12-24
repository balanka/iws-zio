package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
import scala.collection.immutable.{List, Nil}
import java.time.Instant
import zio.prelude.FlipOps

final class PostCustomerInvoiceLive(vatRepo: VatRepository
                                    , accRepo: AccountRepository
                                    , artRepo: ArticleRepository
                                    , customerRepo: CustomerRepository
                                    , repository4PostingTransaction:PostTransactionRepository
                                    , financialsService:FinancialsService)
                                    extends PostCustomerInvoice:

  def articleId2RevenueAccount(articleId: String, articles: List[Article], accounts: List[Account]): Account = 
    filterIWS(articles, articleId).flatMap(art => filterIWS(accounts, art.revenueAccount)).headOption.getOrElse(Account.dummy)
    
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
       _                            <- ZIO.foreachDiscard(transactions.map(_.id))(
                                       id => ZIO.logInfo(s"Posting customer invoice transaction  with id ${id} of company ${transactions.head.company}"))
       post                          <- postTransaction(transactions, company)
       _                             <- ZIO.logInfo(s"Builded transaction while posting customer invoice transaction ${post}")
       nrPostedFinancialsTransaction <- repository4PostingTransaction.post(post._1.map(tr=>tr.copy(posted=true, period=tr.getPeriod))
                , post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
    yield nrPostedFinancialsTransaction

  def buildFinancials(model: Transaction, articles: List[Article], accounts: List[Account], customers: List[BusinessPartner]
                       , vats: List[Vat], modelid: Int): (Transaction, FinancialsTransaction) = {
    // Fetch the customer using its id from the transaction (account field)
    val partnerAccountId = customers.filter(_.id == model.account)
      .flatMap(s => accounts.filter(_.id == s.account))
      .headOption.getOrElse(Account.dummy).id
    val currency = model.lines.headOption.getOrElse(TransactionDetails.dummy).currency
    // build details for vat
    val vatDetails: List[FinancialsTransactionDetails] = model.lines.map { l =>
        val vat = vats.find(vat => vat.id == l.vatCode).getOrElse(Vat.dummy)
        val accountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
        val oaccountName = accounts.find(_.id == vat.outputVatAccount).fold("")(acc => acc.name)
        FinancialsTransactionDetails(-1, 0, partnerAccountId, side = true, vat.outputVatAccount, l.quantity.multiply(l.price).multiply(vat.percent)
          , Instant.now(), l.text, currency, model.company, accountName, oaccountName)
    }.groupBy(line => (line.account, line.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)}.toList
    // build details for net amount
    val netDetails: List[FinancialsTransactionDetails] = model.lines.map { line =>
        val oaccount =  articleId2RevenueAccount(line.article, articles, accounts)
        val accountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
        FinancialsTransactionDetails(-1, 0, partnerAccountId, side = true, oaccount.id, line.quantity.multiply(line.price), Instant.now()
          , model.text, currency, model.company, accountName, oaccount.name)
    }.groupBy(line => (line.account, line.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)}.toList

    val details: List[FinancialsTransactionDetails] = (netDetails ++ vatDetails).filterNot(_.account == FinancialsTransactionDetails.dummy.account)
      .groupBy(d => (d.account, d.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy) }.toList
    val financialsTransaction = FinancialsTransaction(-1, model.id, 0, model.store, partnerAccountId, model.transdate
      , Instant.now(), Instant.now(), model.period, posted = false, modelid, model.company, model.text, -1, -1, details)
    (model.copy(posted = true), financialsTransaction.copy(posted = true))
  }

  private def postTransaction(transactions: List[Transaction], company: Company):
  ZIO[Any, RepositoryError, (List[Transaction], List[FinancialsTransaction], List[PeriodicAccountBalance]
    , ZIO[Any, Nothing, List[PeriodicAccountBalance]], List[TransactionLog], List[Journal], List[Stock], List[Stock], List[Article])] = for {
    accounts <- accRepo.all(Account.MODELID, company.id)
    articles <- artRepo.all(Article.MODELID, company.id)
    customers <- customerRepo.all(Customer.MODELID, company.id)
    vatIds = transactions.flatMap(_.lines.map(_.vatCode)).distinct
    vats <-  vatRepo.getBy(vatIds, Vat.MODEL_ID, company.id)
    newFtr = transactions.map(buildFinancials(_,  articles, accounts, customers, vats, TransactionModelId.RECEIVABLES.id))
    (transactionsx:List[Transaction], financials:List[FinancialsTransaction]) = newFtr.unzip
    result <- postFinancials(financials, financialsService)
    models = result.map(_._1)
    newPacs = result.map(_._2).flatten
    oldPacs = result.map(_._3).flip.map(_.flatten)
    journalEntries = result.map(_._4).flatten
    _<-ZIO.logInfo(s"result2   from  bill of delivery  transaction with  of company ${result}")
    _<-ZIO.logInfo(s"new Pacs   from  bill of delivery  transaction with  of company ${newPacs}")
    _<-ZIO.logInfo(s"Oldoacs   from  bill of delivery  transaction with  of company ${oldPacs}")
  } yield ( transactionsx, models, newPacs, oldPacs, Nil, journalEntries, Nil, Nil, Nil)

object PostCustomerInvoiceLive:
  val live: ZLayer[VatRepository& TransactionRepository& TransactionLogRepository& AccountRepository& CustomerRepository&
     ArticleRepository& PostTransactionRepository &FinancialsService
    , RepositoryError, PostCustomerInvoice] =
    ZLayer.fromFunction(new PostCustomerInvoiceLive(_, _, _, _, _, _))