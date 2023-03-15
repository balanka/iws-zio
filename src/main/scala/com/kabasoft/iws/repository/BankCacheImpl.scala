package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.cache.{Cache, Lookup}
import java.util.concurrent.TimeUnit


final class BankCacheImpl (repository: BankRepository) extends BankCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Bank]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Bank] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Bank](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Bank]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Bank]](repository.getByModelId)).flatMap(_.get(id))
}

object BankCacheImpl {
  val live: ZLayer[BankRepository , RepositoryError, BankCache] =
    ZLayer.fromFunction(new BankCacheImpl(_))
}
