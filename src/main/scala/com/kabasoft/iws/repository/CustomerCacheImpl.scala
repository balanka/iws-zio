package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Customer
import zio._
import zio.cache.{Cache, Lookup}
import java.util.concurrent.TimeUnit

final class CustomerCacheImpl (repository: CustomerRepository) extends CustomerCache  {
  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Customer]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(Id))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Customer] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Customer](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Customer]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Customer]](repository.getByModelId)).flatMap(_.get(id))
}

object CustomerCacheImpl {
  val live: ZLayer[CustomerRepository , RepositoryError, CustomerCache] =
    ZLayer.fromFunction(new CustomerCacheImpl(_))
}