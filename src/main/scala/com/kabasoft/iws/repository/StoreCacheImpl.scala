package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Store
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class StoreCacheImpl (repository: StoreRepository) extends StoreCache  {

  override def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Store]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(Id))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Store] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Store](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Store]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Store]](repository.getByModelId)).flatMap(_.get(id))
}

object StoreCacheImpl {
  val live: ZLayer[StoreRepository , RepositoryError, StoreCache] =
    ZLayer.fromFunction(new StoreCacheImpl(_))
}
