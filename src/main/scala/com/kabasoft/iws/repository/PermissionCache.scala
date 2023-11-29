package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Permission
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait PermissionCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Permission]]
  def getBy(id:(Int,  String)): ZIO[Any, RepositoryError, Permission]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Permission]]

}
object PermissionCache {
  def all(companyId: String): ZIO[PermissionCache, RepositoryError, List[Permission]] =
    ZIO.service[PermissionCache] flatMap (_.all(companyId))

  def getBy(id:(Int, String)): ZIO[PermissionCache, RepositoryError, Permission]=
    ZIO.service[PermissionCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[PermissionCache, RepositoryError, List[Permission]] =
    ZIO.service[PermissionCache] flatMap (_.getByModelId(id))

}
