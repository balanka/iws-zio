package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Vat
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class VatCacheImpl (repository: VatRepository) extends VatCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Vat]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Vat] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Vat](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Vat]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Vat]](repository.getByModelId)).flatMap(_.get(id))
}

object VatCacheImpl {
  val live: ZLayer[VatRepository , RepositoryError, VatCache] =
    ZLayer.fromFunction(new VatCacheImpl(_))
}