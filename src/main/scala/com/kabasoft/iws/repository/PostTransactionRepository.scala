package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, Journal, PeriodicAccountBalance}
import zio._

trait PostTransactionRepository {
  def modifyT(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  //def post(model: FinancialsTransaction, pac2Insert:PeriodicAccountBalance, pac2update:PeriodicAccountBalance,
  //         journal:Journal): ZIO[Any, RepositoryError, Int]
def post(models: List[FinancialsTransaction], pac2Insert: List[PeriodicAccountBalance], pac2update: List[PeriodicAccountBalance],
         journals: List[Journal]): ZIO[Any, RepositoryError, Int]
}
object PostTransactionRepository {

  def modifyT(model: FinancialsTransaction): ZIO[PostTransactionRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PostTransactionRepository](_.modifyT(model))
  //def post(model: FinancialsTransaction, pac2Insert: PeriodicAccountBalance, pac2update: PeriodicAccountBalance,
  //         journal: Journal): ZIO[PostTransactionRepository, RepositoryError, Int] =
 //   ZIO.serviceWithZIO[PostTransactionRepository](_.post(model, pac2Insert, pac2update, journal))

  def post(models: List[FinancialsTransaction], pac2Insert:List[PeriodicAccountBalance], pac2update:List[PeriodicAccountBalance],
           journals:List[Journal]): ZIO[PostTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostTransactionRepository](_.post(models, pac2Insert, pac2update, journals))
}

