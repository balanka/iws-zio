package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Masterfile
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class MasterfileCacheImpl (repository: MasterfileRepository) extends MasterfileCache  {

//  override def all(modelId:Int, companyId: String): ZIO[Any, RepositoryError, List[Masterfile]] = Cache.make(
//    capacity = 100000,
//    timeToLive = Duration.apply(15, TimeUnit.HOURS),
//    lookup = Lookup [(Int, String), Any, RepositoryError, List[Masterfile]] (repository.all).flatMap(_.get(modelId, companyId))

  override def getBy(id:(String, Int, String)): ZIO[Any, RepositoryError, Masterfile] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String, Int, String),Any,RepositoryError, Masterfile](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Masterfile]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Masterfile]](repository.getByModelId)).flatMap(_.get(id))
}

object MasterfileCacheImpl {
  val live: ZLayer[MasterfileRepository , RepositoryError, MasterfileCache] =
    ZLayer.fromFunction(new MasterfileCacheImpl(_))
}
