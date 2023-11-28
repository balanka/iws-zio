package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import zio._
import zio.cache.{Cache, Lookup}
import java.util.concurrent.TimeUnit


final case class FinancialsTransactionCacheImpl (repository: FinancialsTransactionRepository) extends FinancialsTransactionCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  def getByTransId(id:(Long,  String)): ZIO[Any, RepositoryError, FinancialsTransaction]= Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Long, String), Any, RepositoryError, FinancialsTransaction](repository.getByTransId)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[FinancialsTransaction]](repository.getByModelId)).flatMap(_.get(id))
}

object FinancialsTransactionCacheImpl {
  val live: ZLayer[FinancialsTransactionRepository , RepositoryError, FinancialsTransactionCache] =
    ZLayer.fromFunction(new FinancialsTransactionCacheImpl(_))
}
