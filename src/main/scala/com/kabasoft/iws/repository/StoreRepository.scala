package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Store
import zio._
import zio.stream._

trait StoreRepository {
  def create(item: Store): ZIO[Any, RepositoryError, Store]

  def create(models: List[Store]): ZIO[Any, RepositoryError, List[Store]]
  def create2(item: Store): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Store]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Store]]
  def list(Id:(Int,  String)): ZStream[Any, RepositoryError, Store]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, Store]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Store]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Store]
  def modify(model: Store): ZIO[Any, RepositoryError, Int]

}

object StoreRepository {
  def create(item: Store): ZIO[StoreRepository, RepositoryError, Store] =
    ZIO.service[StoreRepository] flatMap (_.create(item))

  def create(items: List[Store]): ZIO[StoreRepository, RepositoryError, List[Store]] =
    ZIO.service[StoreRepository] flatMap (_.create(items))
  def create2(item: Store): ZIO[StoreRepository, RepositoryError, Unit]                               =
    ZIO.service[StoreRepository] flatMap (_.create2(item))
  def create2(items: List[Store]): ZIO[StoreRepository, RepositoryError, Int]                         =
    ZIO.service[StoreRepository] flatMap (_.create2(items))
  def delete(item: String, company: String): ZIO[StoreRepository, RepositoryError, Int]              =
    ZIO.service[StoreRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[StoreRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(Id: (Int,String)): ZIO[StoreRepository, RepositoryError, List[Store]]                      =
    ZIO.service[StoreRepository] flatMap (_.all(Id))
  def list(Id: (Int,String)): ZStream[StoreRepository, RepositoryError, Store]                        =
    ZStream.service[StoreRepository] flatMap (_.list(Id))
  def getBy(id: (String,String)): ZIO[StoreRepository, RepositoryError, Store]               =
    ZIO.service[StoreRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[StoreRepository, RepositoryError, List[Store]]      =
    ZIO.service[StoreRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[StoreRepository, RepositoryError, Store]=
    ZStream.service[StoreRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Store): ZIO[StoreRepository, RepositoryError, Int]                               =
    ZIO.service[StoreRepository] flatMap (_.modify(model))

}
