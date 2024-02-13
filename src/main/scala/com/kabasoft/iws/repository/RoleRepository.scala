package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Role
import zio._
import zio.stream._

trait RoleRepository {
  def create(item: Role): ZIO[Any, RepositoryError, Role]

  def create(models: List[Role]): ZIO[Any, RepositoryError, List[Role]]
  def create2(item: Role): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Role]): ZIO[Any, RepositoryError, Int]
  def delete(id: Int, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Role]]
  def list(Id:(Int, String)): ZStream[Any, RepositoryError, Role]
  def getBy(id: (Int,String)): ZIO[Any, RepositoryError, Role]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Role]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Role]
  def modify(model: Role): ZIO[Any, RepositoryError, Int]

}

object RoleRepository {
  def create(item: Role): ZIO[RoleRepository, RepositoryError, Role] =
    ZIO.service[RoleRepository] flatMap (_.create(item))

  def create(items: List[Role]): ZIO[RoleRepository, RepositoryError, List[Role]] =
    ZIO.service[RoleRepository] flatMap (_.create(items))
  def create2(item: Role): ZIO[RoleRepository, RepositoryError, Unit]                               =
    ZIO.service[RoleRepository] flatMap (_.create2(item))
  def create2(items: List[Role]): ZIO[RoleRepository, RepositoryError, Int]                         =
    ZIO.service[RoleRepository] flatMap (_.create2(items))
  def delete(id: Int, company: String): ZIO[RoleRepository, RepositoryError, Int]              =
    ZIO.service[RoleRepository] flatMap (_.delete(id, company))
  def delete(ids: List[Int], company: String): ZIO[RoleRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(Id:(Int, String)): ZIO[RoleRepository, RepositoryError, List[Role]]                      =
    ZIO.service[RoleRepository] flatMap (_.all(Id))
  def list(Id:(Int, String)): ZStream[RoleRepository, RepositoryError, Role]                        =
    ZStream.service[RoleRepository] flatMap (_.list(Id))
  def getBy(id: (Int,String)): ZIO[RoleRepository, RepositoryError, Role]               =
    ZIO.service[RoleRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[RoleRepository, RepositoryError, List[Role]]      =
    ZIO.service[RoleRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[RoleRepository, RepositoryError, Role]=
    ZStream.service[RoleRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Role): ZIO[RoleRepository, RepositoryError, Int]                               =
    ZIO.service[RoleRepository] flatMap (_.modify(model))

}
