package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Stock
import zio._
import zio.stream._

trait StockRepository:
 def create(item: Stock): ZIO[Any, RepositoryError, Int]
 def create(models: List[Stock]):ZIO[Any, RepositoryError, Int]
 def modify(model: Stock):ZIO[Any, RepositoryError, Int]
 def modify(models: List[Stock]):ZIO[Any, RepositoryError, Int]
 def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Stock]]
 def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, Stock]
 def getByStore(Id: (String, Int, String)): ZIO[Any, RepositoryError, List[Stock]]
 def getByArticle(Id: (String, Int, String)): ZIO[Any, RepositoryError, List[Stock]]
 def getByStoreArticle(Id: (String, String, Int, String)): ZIO[Any, RepositoryError, List[Stock]]
 def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError,List[Stock]]
 def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]
 def deleteAll(): ZIO[Any, RepositoryError, Int]

object StockRepository:

  def create(item: Stock): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.create(item))
    
  def create(models: List[Stock]): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.create(models))

  def modify(model: Stock): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.modify(model))

  def modify(models: List[Stock]): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.all(Id))
    
  def getById(Id: (String, Int, String)): ZIO[StockRepository, RepositoryError, Stock] =
    ZIO.serviceWithZIO[StockRepository](_.getById(Id))

  def getByStore(Id: (String, Int, String)): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.getByStore(Id))

  def getByArticle(Id: (String, Int, String)): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.getByArticle(Id))
    
  def getByStoreArticle(Id: (String, String, Int, String)): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.getByStoreArticle(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.delete(p))

  def deleteAll(): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.deleteAll())