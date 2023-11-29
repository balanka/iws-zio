package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Asset
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class AssetCacheImpl (repository: AssetRepository) extends AssetCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Asset]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Asset] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Asset](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Asset]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Asset]](repository.getByModelId)).flatMap(_.get(id))
}

object AssetCacheImpl {
  val live: ZLayer[AssetRepository , RepositoryError, AssetCache] =
    ZLayer.fromFunction(new AssetCacheImpl(_))
}
