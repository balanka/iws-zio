package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._

final class PostCustomerInvoiceLive(vatRepo: VatRepository
                                    , accRepo: AccountRepository
                                    , artRepo: ArticleRepository
                                    , customerRepo: CustomerRepository
                                    , repository4PostingTransaction:PostTransactionRepository
                                    , repository4PostingFinancialsTransaction:PostFinancialsTransactionRepository )
                                    extends PostCustomerInvoice:
  
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
    if (transactions.isEmpty || transactions.flatMap(_.lines).isEmpty) throw IllegalStateException(" Error: Empty transaction may not be posted!!!")
    for 
       _                            <- ZIO.foreachDiscard(transactions.map(_.id))(
                                       id => ZIO.logInfo(s"Posting customer invoice transaction  with id ${id} of company ${transactions.head.company}"))
       post                          <- postTransaction(transactions, company).map(_.unzip)
       nrPostedTransaction           <- repository4PostingTransaction.post(post._1, Nil, ZIO.succeed(Nil), Nil, Nil, Nil, Nil, Nil )
       nrPostedFinancialsTransaction <- repository4PostingFinancialsTransaction.post(post._2, Nil, ZIO.succeed(Nil), Nil)
    yield nrPostedTransaction + nrPostedFinancialsTransaction

  private def postTransaction(transactions: List[Transaction], company: Company):
                                  ZIO[Any, RepositoryError, List[(Transaction, FinancialsTransaction)]] = for {
    accounts <- accRepo.all(Account.MODELID, company.id)
    customers <- customerRepo.all(Customer.MODELID, company.id)
    articles <- artRepo.getBy(transactions.flatMap(_.lines.map(_.article)), Article.MODELID, company.id)
    vatIds  = articles.map(_.vatCode).distinct
    vats <-  vatRepo.getBy(vatIds, Vat.MODEL_ID, company.id)
    newFTransactions = transactions.map(buildTransaction(_,  accounts, customers
                                  , vats, company.salesClearingAcc, TransactionModelId.RECEIVABLES.id))
  } yield newFTransactions


object PostCustomerInvoiceLive:
  val live: ZLayer[VatRepository& TransactionRepository& TransactionLogRepository& AccountRepository& CustomerRepository&
     ArticleRepository& PostTransactionRepository& PostFinancialsTransactionRepository, RepositoryError, PostCustomerInvoice] =
    ZLayer.fromFunction(new PostCustomerInvoiceLive(_, _, _, _, _,_))