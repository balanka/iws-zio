package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.UserRole
import zio._
import zio.stream._

trait RoleRepository {
  def create(item: UserRole): ZIO[Any, RepositoryError, UserRole]

  def create(models: List[UserRole]): ZIO[Any, RepositoryError, List[UserRole]]
  def create2(item: UserRole): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[UserRole]): ZIO[Any, RepositoryError, Int]
  def delete(id: Int, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[UserRole]]
  def list(company: String): ZStream[Any, RepositoryError, UserRole]
  def getBy(id: (Int,String)): ZIO[Any, RepositoryError, UserRole]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[UserRole]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, UserRole]
  def modify(model: UserRole): ZIO[Any, RepositoryError, Int]

}

object RoleRepository {
  def create(item: UserRole): ZIO[RoleRepository, RepositoryError, UserRole] =
    ZIO.service[RoleRepository] flatMap (_.create(item))

  def create(items: List[UserRole]): ZIO[RoleRepository, RepositoryError, List[UserRole]] =
    ZIO.service[RoleRepository] flatMap (_.create(items))
  def create2(item: UserRole): ZIO[RoleRepository, RepositoryError, Unit]                               =
    ZIO.service[RoleRepository] flatMap (_.create2(item))
  def create2(items: List[UserRole]): ZIO[RoleRepository, RepositoryError, Int]                         =
    ZIO.service[RoleRepository] flatMap (_.create2(items))
  def delete(id: Int, company: String): ZIO[RoleRepository, RepositoryError, Int]              =
    ZIO.service[RoleRepository] flatMap (_.delete(id, company))
  def delete(ids: List[Int], company: String): ZIO[RoleRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(companyId: String): ZIO[RoleRepository, RepositoryError, List[UserRole]]                      =
    ZIO.service[RoleRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[RoleRepository, RepositoryError, UserRole]                        =
    ZStream.service[RoleRepository] flatMap (_.list(company))
  def getBy(id: (Int,String)): ZIO[RoleRepository, RepositoryError, UserRole]               =
    ZIO.service[RoleRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[RoleRepository, RepositoryError, List[UserRole]]      =
    ZIO.service[RoleRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[RoleRepository, RepositoryError, UserRole]=
    ZStream.service[RoleRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: UserRole): ZIO[RoleRepository, RepositoryError, Int]                               =
    ZIO.service[RoleRepository] flatMap (_.modify(model))

}
