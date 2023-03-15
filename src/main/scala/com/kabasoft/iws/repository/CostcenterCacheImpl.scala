package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Costcenter
import zio._
import zio.cache.{Cache, Lookup}
import java.util.concurrent.TimeUnit
final class CostcenterCacheImpl (repository: CostcenterRepository) extends CostcenterCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Costcenter]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Costcenter] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Costcenter](repository.getBy)).flatMap(_.get(id))
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Costcenter]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Costcenter]](repository.getByModelId)).flatMap(_.get(id))
}

object CostcenterCacheImpl {
  val live: ZLayer[CostcenterRepository , RepositoryError, CostcenterCache] =
    ZLayer.fromFunction(new CostcenterCacheImpl(_))
}