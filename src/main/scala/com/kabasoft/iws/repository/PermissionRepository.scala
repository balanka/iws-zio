package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Permission
import zio._
import zio.stream._

trait PermissionRepository {
  def create(item: Permission): ZIO[Any, RepositoryError, Permission]

  def create(models: List[Permission]): ZIO[Any, RepositoryError, List[Permission]]
  def create2(item: Permission): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Permission]): ZIO[Any, RepositoryError, Int]
  def delete(id: Int, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[Permission]]
  def list(company: String): ZStream[Any, RepositoryError, Permission]
  def getBy(id: (Int,String)): ZIO[Any, RepositoryError, Permission]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Permission]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Permission]
  def modify(model: Permission): ZIO[Any, RepositoryError, Int]

}

object PermissionRepository {
  def create(item: Permission): ZIO[PermissionRepository, RepositoryError, Permission] =
    ZIO.service[PermissionRepository] flatMap (_.create(item))

  def create(items: List[Permission]): ZIO[PermissionRepository, RepositoryError, List[Permission]] =
    ZIO.service[PermissionRepository] flatMap (_.create(items))
  def create2(item: Permission): ZIO[PermissionRepository, RepositoryError, Unit]                               =
    ZIO.service[PermissionRepository] flatMap (_.create2(item))
  def create2(items: List[Permission]): ZIO[PermissionRepository, RepositoryError, Int]                         =
    ZIO.service[PermissionRepository] flatMap (_.create2(items))
  def delete(id: Int, company: String): ZIO[PermissionRepository, RepositoryError, Int]              =
    ZIO.service[PermissionRepository] flatMap (_.delete(id, company))
  def delete(ids: List[Int], company: String): ZIO[PermissionRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(companyId: String): ZIO[PermissionRepository, RepositoryError, List[Permission]]                      =
    ZIO.service[PermissionRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[PermissionRepository, RepositoryError, Permission]                        =
    ZStream.service[PermissionRepository] flatMap (_.list(company))
  def getBy(id: (Int,String)): ZIO[PermissionRepository, RepositoryError, Permission]               =
    ZIO.service[PermissionRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[PermissionRepository, RepositoryError, List[Permission]]      =
    ZIO.service[PermissionRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[PermissionRepository, RepositoryError, Permission]=
    ZStream.service[PermissionRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Permission): ZIO[PermissionRepository, RepositoryError, Int]                               =
    ZIO.service[PermissionRepository] flatMap (_.modify(model))

}
