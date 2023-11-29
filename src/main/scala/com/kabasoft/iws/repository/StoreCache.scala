package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Store
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait StoreCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Store]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Store]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Store]]

}
object StoreCache {
  def all(companyId: String): ZIO[StoreCache, RepositoryError, List[Store]] =
    ZIO.service[StoreCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[StoreCache, RepositoryError, Store]=
    ZIO.service[StoreCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[StoreCache, RepositoryError, List[Store]] =
    ZIO.service[StoreCache] flatMap (_.getByModelId(id))

}
