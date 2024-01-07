package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.SalaryItem
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class SalaryItemCacheImpl (repository: SalaryItemRepository) extends SalaryItemCache  {

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[SalaryItem]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(Id))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, SalaryItem] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, SalaryItem](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[SalaryItem]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[SalaryItem]](repository.getByModelId)).flatMap(_.get(id))
}

object SalaryItemCacheImpl {
  val live: ZLayer[SalaryItemRepository , RepositoryError, SalaryItemCache] =
    ZLayer.fromFunction(new SalaryItemCacheImpl(_))
}
