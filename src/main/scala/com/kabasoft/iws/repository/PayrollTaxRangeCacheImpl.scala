package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PayrollTaxRange
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class PayrollTaxRangeCacheImpl (repository: PayrollTaxRangeRepository) extends PayrollTaxRangeCache  {

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(Id))

  override def getBy(id:(String, Int, String)): ZIO[Any, RepositoryError, PayrollTaxRange] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[PayrollTaxRange]](repository.getByModelId)).flatMap(_.get(id))
}

object PayrollTaxRangeCacheImpl {
  val live: ZLayer[PayrollTaxRangeRepository , RepositoryError, PayrollTaxRangeCache] =
    ZLayer.fromFunction(new PayrollTaxRangeCacheImpl(_))
}
