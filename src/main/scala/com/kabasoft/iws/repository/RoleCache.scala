package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Role
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait RoleCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Role]]
  def getBy(id:(Int,  String)): ZIO[Any, RepositoryError, Role]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Role]]

}
object RoleCache {
  def all(companyId: String): ZIO[RoleCache, RepositoryError, List[Role]] =
    ZIO.service[RoleCache] flatMap (_.all(companyId))

  def getBy(id:(Int, String)): ZIO[RoleCache, RepositoryError, Role]=
    ZIO.service[RoleCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[RoleCache, RepositoryError, List[Role]] =
    ZIO.service[RoleCache] flatMap (_.getByModelId(id))

}
