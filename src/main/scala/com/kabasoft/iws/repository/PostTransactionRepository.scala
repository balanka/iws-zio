package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Article, Journal, PeriodicAccountBalance, Stock, Transaction, TransactionLog}
import zio._

trait PostTransactionRepository {
def post(models: List[Transaction], pac2Insert: List[PeriodicAccountBalance], pac2update: UIO[List[PeriodicAccountBalance]],
         transLogEntries:List[TransactionLog], journals: List[Journal], stocks:List[Stock], newStock:List[Stock], articles: List[Article]): ZIO[Any, RepositoryError, Int]
}
object PostTransactionRepository {

  def post(models: List[Transaction], pac2Insert:List[PeriodicAccountBalance], pac2update:UIO[List[PeriodicAccountBalance]], transLogEntries:List[TransactionLog],
           journals:List[Journal], stocks:List[Stock], newStock:List[Stock], articles: List[Article]): ZIO[PostTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostTransactionRepository](_.post(models, pac2Insert, pac2update, transLogEntries, journals, stocks, newStock, articles))
}

