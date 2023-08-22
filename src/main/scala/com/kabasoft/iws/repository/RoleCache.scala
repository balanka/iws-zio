package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.UserRole
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait RoleCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[UserRole]]
  def getBy(id:(Int,  String)): ZIO[Any, RepositoryError, UserRole]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[UserRole]]

}
object RoleCache {
  def all(companyId: String): ZIO[RoleCache, RepositoryError, List[UserRole]] =
    ZIO.service[RoleCache] flatMap (_.all(companyId))

  def getBy(id:(Int, String)): ZIO[RoleCache, RepositoryError, UserRole]=
    ZIO.service[RoleCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[RoleCache, RepositoryError, List[UserRole]] =
    ZIO.service[RoleCache] flatMap (_.getByModelId(id))

}
