package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common._
import com.kabasoft.iws.repository._
import zio._
import java.time.Instant

final class PostSupplierInvoiceImpl(vatRepo: VatRepository
                                  , accRepo: AccountRepository
                                  , artRepo: ArticleRepository
                                  , supplierRepo: SupplierRepository
                                  , repository4PostingTransaction:PostTransactionRepository
                                   ,repository4PostingFinancialsTransaction:PostFinancialsTransactionRepository )
                                    extends PostSupplierInvoice {

  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
    if (transactions.isEmpty) ZIO.succeed(0) else for {
    _ <- ZIO.foreachDiscard(transactions.map(_.id))(
      id => ZIO.logInfo(s"Posting supplier invoice transaction  with id ${id} of company ${transactions.head.company}"))
    post <- postTransaction(transactions, company).map(_.unzip)
    nrPostedTransaction <- repository4PostingTransaction.post(post._1, Nil, ZIO.succeed(Nil), Nil, Nil, Nil, Nil, Nil )
    nrPostedFinancialsTransaction <- repository4PostingFinancialsTransaction.post(post._2, Nil, ZIO.succeed(Nil), Nil)
    } yield nrPostedTransaction + nrPostedFinancialsTransaction

  private[this] def postTransaction(transactions: List[Transaction], company: Company): ZIO[Any, RepositoryError, List[(Transaction, FinancialsTransaction)]] = for {

    accounts <- accRepo.all(Account.MODELID, company.id)
    suppliers <- supplierRepo.all(Supplier.MODELID, company.id)
    articles <- artRepo.getBy(transactions.flatMap(m => m.lines.map(_.article)), company.id)
    vats <-  vatRepo.getBy(articles.map(_.vatCode), company.id)
    newFTransactions = transactions.map(tr => buildPacsFromTransaction(tr,  accounts, suppliers, vats, company.purchasingClearingAcc))

  } yield newFTransactions




  def buildPacsFromTransaction(model:Transaction,  accounts:List[Account], suppliers:List[Supplier]
                                , vats:List[Vat], accountId: String): (Transaction, FinancialsTransaction)  = {
    val supplierAccountId = suppliers.filter(_.id == model.costcenter)
                                     .flatMap(s=>accounts.filter(_.id==s.account))
                                     .headOption.getOrElse(Account.dummy).id

    val currency = model.lines.headOption.getOrElse(TransactionDetails.dummy).currency
    val details0 = model.lines.map { l=>
      val vat = vats.find(vat=> vat.id == l.vatCode).getOrElse(Vat.dummy)
      FinancialsTransactionDetails(-1, 0, supplierAccountId, side = true, vat.inputVatAccount, model.total.multiply(vat.percent), Instant.now(), model.text, currency, "", "")
    }
    val vatAmount = reduce(details0.map(_.amount), zeroAmount)
    val netAmount = model.total.subtract(vatAmount)
    val details= details0.::(FinancialsTransactionDetails(-1, 0, supplierAccountId, side = true, accountId, netAmount, Instant.now(), model.text, currency, "", ""))
   val financialsTransaction = FinancialsTransaction(-1, model.id, 0, model.store, supplierAccountId,  model.transdate, Instant.now(), Instant.now()
      , model.period, posted = false,  TransactionModelId.SUPPLIER_INVOICE.id, model.company, model.text, -1, -1, details)
    (model.copy(posted = true), financialsTransaction)

  }


}

object PostSupplierInvoiceImpl {
  val live: ZLayer[VatRepository with TransactionRepository with TransactionLogRepository with AccountRepository with SupplierRepository
    with ArticleRepository  with PostTransactionRepository  with PostFinancialsTransactionRepository, RepositoryError, PostSupplierInvoice] =
    ZLayer.fromFunction(new PostSupplierInvoiceImpl(_, _, _, _, _,_))
}