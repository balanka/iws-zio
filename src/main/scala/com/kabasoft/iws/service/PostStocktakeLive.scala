package com.kabasoft.iws.service

import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository._
import zio._


final class PostStocktakeLive(accRepo: AccountRepository
                                  , artRepo: ArticleRepository
                                  , stockRepo: StockRepository
                                  , financialsService:FinancialsService
                                  , repository4PostingTransaction:PostTransactionRepository)
                                    extends PostStocktake:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
    postAll(transactions, company, stockRepo, accRepo, artRepo, financialsService, repository4PostingTransaction)

object PostStocktakeLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& AccountRepository& ArticleRepository
    & StockRepository& PostTransactionRepository&FinancialsService, RepositoryError, PostStocktake] =
    ZLayer.fromFunction(new PostStocktakeLive(_, _, _, _, _))
