package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Employee
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait EmployeeCache {
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Employee]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Employee]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Employee]]

}
object EmployeeCache {
  def all(Id:(Int, String)): ZIO[EmployeeCache, RepositoryError, List[Employee]] =
    ZIO.service[EmployeeCache] flatMap (_.all(Id))

  def getBy(id:(String, String)): ZIO[EmployeeCache, RepositoryError, Employee]=
    ZIO.service[EmployeeCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[EmployeeCache, RepositoryError, List[Employee]] =
    ZIO.service[EmployeeCache] flatMap (_.getByModelId(id))

}