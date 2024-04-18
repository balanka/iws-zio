package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Transaction
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final case class TransactionCacheImpl (repository: TransactionRepository) extends TransactionCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Transaction]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  def getByTransId(id:(Long,  String)): ZIO[Any, RepositoryError, Transaction]= Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Long, String), Any, RepositoryError, Transaction](repository.getByTransId)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Transaction]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Transaction]](repository.getByModelId)).flatMap(_.get(id))
}

object TransactionCacheImpl {
  val live: ZLayer[TransactionRepository , RepositoryError, TransactionCache] =
    ZLayer.fromFunction(new TransactionCacheImpl(_))
}
