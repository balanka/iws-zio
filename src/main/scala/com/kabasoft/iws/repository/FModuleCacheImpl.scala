package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Fmodule
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class FModuleCacheImpl (repository: FModuleRepository) extends FModuleCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Fmodule]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(Int, String)): ZIO[Any, RepositoryError, Fmodule] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int,String),Any,RepositoryError, Fmodule](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Fmodule]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Fmodule]](repository.getByModelId)).flatMap(_.get(id))
}

object FModuleCacheImpl {
  val live: ZLayer[FModuleRepository , RepositoryError, FModuleCache] =
    ZLayer.fromFunction(new FModuleCacheImpl(_))
}
