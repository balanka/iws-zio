package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Permission
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class PermissionCacheImpl (repository: PermissionRepository) extends PermissionCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Permission]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(Int, String)): ZIO[Any, RepositoryError, Permission] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int,String),Any,RepositoryError, Permission](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Permission]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Permission]](repository.getByModelId)).flatMap(_.get(id))
}

object PermissionCacheImpl {
  val live: ZLayer[PermissionRepository , RepositoryError, PermissionCache] =
    ZLayer.fromFunction(new PermissionCacheImpl(_))
}
