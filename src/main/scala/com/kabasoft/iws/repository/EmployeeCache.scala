package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Employee
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait EmployeeCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Employee]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Employee]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Employee]]

}
object EmployeeCache {
  def all(companyId: String): ZIO[EmployeeCache, RepositoryError, List[Employee]] =
    ZIO.service[EmployeeCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[EmployeeCache, RepositoryError, Employee]=
    ZIO.service[EmployeeCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[EmployeeCache, RepositoryError, List[Employee]] =
    ZIO.service[EmployeeCache] flatMap (_.getByModelId(id))

}