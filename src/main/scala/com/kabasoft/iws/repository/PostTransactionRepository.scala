package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Article, FinancialsTransaction, Journal, PeriodicAccountBalance, Stock, Transaction, TransactionLog}
import zio.*

trait PostTransactionRepository:
   def post(models: List[Transaction], financials: List[FinancialsTransaction]
         , transLogEntries:List[TransactionLog] 
         , stocks:List[Stock], newStock:List[Stock]
         , articles: List[Article]): ZIO[Any, RepositoryError, Int]
  
   def post(models: List[Transaction], financials: List[FinancialsTransaction]
         , newPacs: List[PeriodicAccountBalance]
         , oldPacs: ZIO[Any, Nothing,List[PeriodicAccountBalance]]
         , transLogEntries: List[TransactionLog]
         , journals: List[Journal]
         , stocks: List[Stock], newStock: List[Stock]
         , articles: List[Article]): ZIO[Any, RepositoryError, Int]

object PostTransactionRepository:
  def post(models: List[Transaction], financials: List[FinancialsTransaction]
           , transLogEntries: List[TransactionLog]
           , stocks: List[Stock], newStock: List[Stock]
           , articles: List[Article]): ZIO[PostTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PostTransactionRepository](_.post(models, financials, transLogEntries, stocks, newStock, articles))
    
  def post(models: List[Transaction], financials: List[FinancialsTransaction]
           , newPacs:List[PeriodicAccountBalance]
           , oldPacs: ZIO[Any, Nothing,List[PeriodicAccountBalance]]
           , transLogEntries:List[TransactionLog]
           , stocks:List[Stock], newStock:List[Stock]
           , journals: List[Journal]
           , articles: List[Article]): ZIO[PostTransactionRepository, RepositoryError, Int] =
               ZIO.serviceWithZIO[PostTransactionRepository](_.post(models,  financials,  newPacs, oldPacs
                 , transLogEntries, journals, stocks, newStock, articles))
