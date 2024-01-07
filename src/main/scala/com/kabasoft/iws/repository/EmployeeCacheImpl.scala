package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Employee
import zio._
import zio.cache.{Cache, Lookup}

import java.util.concurrent.TimeUnit

final class EmployeeCacheImpl (repository: EmployeeRepository) extends EmployeeCache  {
  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Employee]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup(repository.all)).flatMap(_.get(Id))

  override def getBy(id:(String, String)): ZIO[Any, RepositoryError, Employee] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(String,String),Any,RepositoryError, Employee](repository.getBy)).flatMap(_.get(id))

  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Employee]] = Cache.make(
    capacity = 100000,
    timeToLive = Duration.apply(15, TimeUnit.HOURS),
    lookup = Lookup[(Int, String), Any, RepositoryError, List[Employee]](repository.getByModelId)).flatMap(_.get(id))
}

object EmployeeCacheImpl {
  val live: ZLayer[EmployeeRepository , RepositoryError, EmployeeCache] =
    ZLayer.fromFunction(new EmployeeCacheImpl(_))
}
