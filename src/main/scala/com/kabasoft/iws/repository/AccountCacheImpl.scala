package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Account
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final case class AccountCacheImpl (repository: AccountRepository) extends AccountCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Account]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Account] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Account](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Account]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Account]](repository.getByModelId)).flatMap(_.get(id))
}

object AccountCacheImpl {
  val live: ZLayer[AccountRepository , RepositoryError, AccountCache] =
    ZLayer.fromFunction(new AccountCacheImpl(_))
}
