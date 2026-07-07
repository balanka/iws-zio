package com.kabasoft.iws.service

import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository._
import zio._


final class PostConsumptionLive(accRepo: AccountRepository
                                  , artRepo: ArticleRepository
                                  , stockRepo: StockRepository
                                  , financialsService:FinancialsService
                                  , repository4PostingTransaction:PostTransactionRepository)
                                    extends PostConsumption:
  override def postAll(transactions: List[Transaction], company:Company): ZIO[Any, RepositoryError, Int]  =
    postAll(transactions, company, stockRepo, accRepo, artRepo, financialsService, repository4PostingTransaction)

object PostConsumptionLive:
  val live: ZLayer[TransactionRepository& TransactionLogRepository& AccountRepository&
      ArticleRepository& StockRepository& PostTransactionRepository&FinancialsService, RepositoryError, PostConsumption] =
    ZLayer.fromFunction(new PostConsumptionLive(_, _, _, _, _))
