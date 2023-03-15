package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Supplier
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class SupplierCacheImpl (repository: SupplierRepository) extends SupplierCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Supplier]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Supplier] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Supplier](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Supplier]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Supplier]](repository.getByModelId)).flatMap(_.get(id))
}

object SupplierCacheImpl {
  val live: ZLayer[SupplierRepository , RepositoryError, SupplierCache] =
    ZLayer.fromFunction(new SupplierCacheImpl(_))
}
