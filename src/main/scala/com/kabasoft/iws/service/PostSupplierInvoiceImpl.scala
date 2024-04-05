package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
final class PostSupplierInvoiceImpl(vatRepo: VatRepository
                                  , accRepo: AccountRepository
                                  , artRepo: ArticleRepository
                                  , supplierRepo: SupplierRepository
                                  , repository4PostingTransaction:PostTransactionRepository
                                  , repository4PostingFinancialsTransaction:PostFinancialsTransactionRepository )
                                    extends PostSupplierInvoice {
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
    if (transactions.isEmpty) ZIO.succeed(0) else for {
    _ <- ZIO.foreachDiscard(transactions.map(_.id))(
      id => ZIO.logDebug(s"Posting supplier invoice transaction  with id ${id} of company ${transactions.head.company}"))
    post <- postTransaction(transactions, company).map(_.unzip)
    nrPostedTransaction <- repository4PostingTransaction.post(post._1, Nil, ZIO.succeed(Nil), Nil, Nil, Nil, Nil, Nil )
    nrPostedFinancialsTransaction <- repository4PostingFinancialsTransaction.post(post._2, Nil, ZIO.succeed(Nil), Nil)
    } yield nrPostedTransaction + nrPostedFinancialsTransaction

  private[this] def postTransaction(transactions: List[Transaction], company: Company):
                     ZIO[Any, RepositoryError, List[(Transaction, FinancialsTransaction)]] = for {
    accounts <- accRepo.all(Account.MODELID, company.id)
    suppliers <- supplierRepo.all(Supplier.MODELID, company.id)
    articles <- artRepo.getBy(transactions.flatMap(m => m.lines.map(_.article)), company.id)
    vatIds  = articles.map(_.vatCode).distinct
    vats <-  vatRepo.getBy(vatIds, company.id)
    newFTransactions = transactions.map(buildTransaction(_,  accounts, suppliers, vats
                                       , company.purchasingClearingAcc, TransactionModelId.PAYABLES.id))
  } yield newFTransactions
}
object PostSupplierInvoiceImpl {
  val live: ZLayer[VatRepository with TransactionRepository with TransactionLogRepository with AccountRepository with SupplierRepository
    with ArticleRepository  with PostTransactionRepository  with PostFinancialsTransactionRepository, RepositoryError, PostSupplierInvoice] =
    ZLayer.fromFunction(new PostSupplierInvoiceImpl(_, _, _, _, _,_))
}