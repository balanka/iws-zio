package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.ImportFile
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class ImportFileCacheImpl (repository: ImportFileRepository) extends ImportFileCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[ImportFile]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, ImportFile] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, ImportFile](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[ImportFile]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[ImportFile]](repository.getByModelId)).flatMap(_.get(id))
}

object ImportFileCacheImpl {
  val live: ZLayer[ImportFileRepository , RepositoryError, ImportFileCache] =
    ZLayer.fromFunction(new ImportFileCacheImpl(_))
}
