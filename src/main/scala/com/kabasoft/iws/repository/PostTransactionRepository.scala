package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Journal, PeriodicAccountBalance, Stock, Transaction}
import zio._

trait PostTransactionRepository {
def post(models: List[Transaction], pac2Insert: List[PeriodicAccountBalance], pac2update: UIO[List[PeriodicAccountBalance]],
         journals: List[Journal], stocks:(List[Stock], List[Stock]) ): ZIO[Any, RepositoryError, Int]
}
object PostTransactionRepository {

  def post(models: List[Transaction], pac2Insert:List[PeriodicAccountBalance], pac2update:UIO[List[PeriodicAccountBalance]],
           journals:List[Journal], stocks:(List[Stock], List[Stock])): ZIO[PostTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostTransactionRepository](_.post(models, pac2Insert, pac2update, journals, stocks))
}

