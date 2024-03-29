package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Module
import zio._
import zio.cache.{Cache, Lookup}
import java.util.concurrent.TimeUnit


final class ModuleCacheImpl (repository: ModuleRepository) extends ModuleCache  {

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Module]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(Id))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Module] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Module](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Module]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Module]](repository.getByModelId)).flatMap(_.get(id))
}

object ModuleCacheImpl {
  val live: ZLayer[ModuleRepository , RepositoryError, ModuleCache] =
    ZLayer.fromFunction(new ModuleCacheImpl(_))
}
