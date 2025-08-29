package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
import scala.collection.immutable.{List, Nil}
import java.time.Instant
import zio.prelude.FlipOps
final class PostSupplierInvoiceLive(vatRepo: VatRepository
                                    , accRepo: AccountRepository
                                    , artRepo: ArticleRepository
                                    , supplierRepo: SupplierRepository
                                    , repository4PostingTransaction:PostTransactionRepository
                                    , financialsService:FinancialsService)
                                    extends PostSupplierInvoice:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
   if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
   for {
     _ <- ZIO.foreachDiscard(transactions.map(_.id))(
       id => ZIO.logInfo(s"Posting supplier invoice transaction  with id ${id} of company ${transactions.head.company}"))
     post <- postTransaction(transactions, company)
     _ <-ZIO.logInfo(s" supplier invoice transaction  ${post._1}")
     _ <-ZIO.logInfo(s" Created supplier invoice financials transaction  ${post._2}")
     nrPostedTransaction <- repository4PostingTransaction.post(post._1.map(tr=>tr.copy(posted=true, period=tr.getPeriod))
       , post._2, post._3, post._4, post._5, post._6, post._7, post._8, post._9)
   } yield nrPostedTransaction


  def buildFinancials(model: Transaction, accounts: List[Account], suppliers: List[BusinessPartner]
                       , vats: List[Vat], oaccountId: String, modelid: Int): (Transaction, FinancialsTransaction) = {
    // Fetch the supplier using its id from the transaction (account field)
    val partnerAccountId = suppliers.filter(_.id == model.account)
      .flatMap(s => accounts.filter(_.id == s.account))
      .headOption.getOrElse(Account.dummy).id
    val currency = model.lines.headOption.getOrElse(TransactionDetails.dummy).currency
    // build details for vat
    val vatDetails: List[FinancialsTransactionDetails] = model.lines.map { l =>
      val vat = vats.find(vat => vat.id == l.vatCode).getOrElse(Vat.dummy)
      val accountName = accounts.find(_.id == vat.inputVatAccount).fold("")(acc => acc.name)
      val oaccountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
      FinancialsTransactionDetails(-1, 0, vat.inputVatAccount, side = true, partnerAccountId, l.quantity.multiply(l.price).multiply(vat.percent)
        , Instant.now(), l.text, currency, model.company, accountName, oaccountName)
    }.groupBy(line => (line.account, line.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)
    }.toList
    // build details for net amount
    val netDetails: List[FinancialsTransactionDetails] = model.lines.map { line =>
      val accountId = oaccountId
      val accountName = accounts.find(_.id == accountId).fold("")(acc => acc.name)
      val oaccountName = accounts.find(_.id == partnerAccountId).fold("")(acc => acc.name)
      FinancialsTransactionDetails(-1, 0, accountId, side = true, partnerAccountId, line.quantity.multiply(line.price), Instant.now()
        , model.text, currency, model.company, accountName, oaccountName)
    }.groupBy(line => (line.account, line.oaccount)).map { case (_, v) => common.reduce(v, FinancialsTransactionDetails.dummy)
    }.toList

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
    suppliers <- supplierRepo.all(Supplier.MODELID, company.id)
    _ <-ZIO.logInfo(s"Posting supplier invoice transaction  4  suppliers  ${suppliers}")
    vatIds = transactions.flatMap(_.lines.map(_.vatCode)).distinct
    vats <-  vatRepo.getBy(vatIds, Vat.MODEL_ID, company.id)
    newFtr = transactions.map(buildFinancials(_, accounts, suppliers, vats, company.purchasingClearingAcc
      , TransactionModelId.PAYABLES.id))
    (transactionsx:List[Transaction], financials:List[FinancialsTransaction]) = newFtr.unzip
    result <- postFinancials(financials, financialsService)
    models = result.map(_._1)
    newPacs = result.map(_._2).flatten
    oldPacs = result.map(_._3).flip.map(_.flatten)
    journalEntries = result.map(_._4).flatten
    _<-ZIO.logInfo(s"result   from  supplier invoice  transaction with  of company ${result}")
    _<-ZIO.logInfo(s"new Pacs   from  supplier invoice  transaction with  of company ${newPacs}")
    _<-ZIO.logInfo(s"Oldoacs   from  supplier invoice  transaction with  of company ${oldPacs}")
  } yield ( transactionsx, models, newPacs, oldPacs, Nil, journalEntries, Nil, Nil, Nil)

object PostSupplierInvoiceLive:
  val live: ZLayer[VatRepository& TransactionRepository& TransactionLogRepository& AccountRepository& SupplierRepository
    & ArticleRepository& PostTransactionRepository& FinancialsService, RepositoryError, PostSupplierInvoice] =
    ZLayer.fromFunction(new PostSupplierInvoiceLive(_, _, _, _, _, _))
