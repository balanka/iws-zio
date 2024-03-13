package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, Journal, PeriodicAccountBalance}
import zio._

trait PostFinancialsTransactionRepository {
def post(models: List[FinancialsTransaction], pac2Insert: List[PeriodicAccountBalance], pac2update: UIO[List[PeriodicAccountBalance]],
         journals: List[Journal]): ZIO[Any, RepositoryError, Int]
}
object PostFinancialsTransactionRepository {

  def post(models: List[FinancialsTransaction], pac2Insert:List[PeriodicAccountBalance], pac2update:UIO[List[PeriodicAccountBalance]],
           journals:List[Journal]): ZIO[PostFinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostFinancialsTransactionRepository](_.post(models, pac2Insert, pac2update, journals))
}

