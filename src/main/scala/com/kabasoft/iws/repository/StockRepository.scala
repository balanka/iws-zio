package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Stock
import zio._
import zio.stream._

trait StockRepository:
 def create(item: Stock, flag: Boolean): ZIO[Any, RepositoryError, Int]
 def create(models: List[Stock]):ZIO[Any, RepositoryError, Int]
 def modify(model: Stock):ZIO[Any, RepositoryError, Int]
 def modify(models: List[Stock]):ZIO[Any, RepositoryError, Int]
 def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Stock]]
 def getById(Id: (String, String, Int, String)): ZIO[Any, RepositoryError, List[Stock]]
 def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError,List[Stock]]
 def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]

object StockRepository:

  def create(item: Stock, flag: Boolean): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.create(item, flag))
    
  def create(models: List[Stock]): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.create(models))

  def modify(model: Stock): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.modify(model))

  def modify(models: List[Stock]): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.all(Id))

  def getById(Id: (String, String, Int, String)): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.serviceWithZIO[StockRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[StockRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StockRepository](_.delete(p))

