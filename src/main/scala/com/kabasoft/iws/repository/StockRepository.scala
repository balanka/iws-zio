package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Stock
import zio._
import zio.stream._

trait StockRepository {
  def create(item: Stock): ZIO[Any, RepositoryError, Stock]

  def create(models: List[Stock]): ZIO[Any, RepositoryError, List[Stock]]
  def create2(item: Stock): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Stock]): ZIO[Any, RepositoryError, Int]
  def delete(store: String, article: String,company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[(String, String)], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(ids=>delete(ids._1, ids._2, company)))
  def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Stock]]
  def list(Id:(Int,  String)): ZStream[Any, RepositoryError, Stock]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, List[Stock]]
  def getBy(id: (String,String, String)): ZIO[Any, RepositoryError, Stock]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Stock]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Stock]
  def modify(model: Stock): ZIO[Any, RepositoryError, Int]

}

object StockRepository {
  def create(item: Stock): ZIO[StockRepository, RepositoryError, Stock] =
    ZIO.service[StockRepository] flatMap (_.create(item))

  def create(items: List[Stock]): ZIO[StockRepository, RepositoryError, List[Stock]] =
    ZIO.service[StockRepository] flatMap (_.create(items))
  def create2(item: Stock): ZIO[StockRepository, RepositoryError, Unit]                               =
    ZIO.service[StockRepository] flatMap (_.create2(item))
  def create2(items: List[Stock]): ZIO[StockRepository, RepositoryError, Int]                         =
    ZIO.service[StockRepository] flatMap (_.create2(items))
  def delete(store: String, article: String, company: String): ZIO[StockRepository, RepositoryError, Int]              =
    ZIO.service[StockRepository] flatMap (_.delete(store, article, company))

  def all(Id: (Int,String)): ZIO[StockRepository, RepositoryError, List[Stock]]                      =
    ZIO.service[StockRepository] flatMap (_.all(Id))
  def list(Id: (Int,String)): ZStream[StockRepository, RepositoryError, Stock]                        =
    ZStream.service[StockRepository] flatMap (_.list(Id))
  def getBy(id: (String,String)): ZIO[StockRepository, RepositoryError, List[Stock]]               =
    ZIO.service[StockRepository] flatMap (_.getBy(id))
  def getBy(id: (String,String, String)): ZIO[StockRepository, RepositoryError, Stock]               =
    ZIO.service[StockRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[StockRepository, RepositoryError, List[Stock]]      =
    ZIO.service[StockRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[StockRepository, RepositoryError, Stock]=
    ZStream.service[StockRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Stock): ZIO[StockRepository, RepositoryError, Int]                               =
    ZIO.service[StockRepository] flatMap (_.modify(model))

}
