package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Role
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit


final class RoleCacheImpl (repository: RoleRepository) extends RoleCache  {

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Role]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(companyId))

  override def getBy(id:(Int, String)): ZIO[Any, RepositoryError, Role] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int,String),Any,RepositoryError, Role](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Role]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Role]](repository.getByModelId)).flatMap(_.get(id))
}

object RoleCacheImpl {
  val live: ZLayer[RoleRepository , RepositoryError, RoleCache] =
    ZLayer.fromFunction(new RoleCacheImpl(_))
}
