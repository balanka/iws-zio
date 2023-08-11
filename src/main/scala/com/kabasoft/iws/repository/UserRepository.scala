package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.User
import zio._
import zio.stream._

trait UserRepository {
  def create(item: User): ZIO[Any, RepositoryError, User]
  def create(models: List[User]): ZIO[Any, RepositoryError, List[User]]
  def create2(item: User): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[User]): ZIO[Any, RepositoryError, Int]
  def delete(id: Int, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Int], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[User]]
  def list(company: String): ZStream[Any, RepositoryError, User]
  def getByUserName(userName: String, company: String): ZIO[Any, RepositoryError, User]
  def getById(userId: Int, companyId: String): ZIO[Any, RepositoryError, User]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, User]
  def modify(model: User): ZIO[Any, RepositoryError, Int]
  def modify(models: List[User]): ZIO[Any, RepositoryError, Int]
}

object UserRepository {
  def create(item: User): ZIO[UserRepository, RepositoryError, User] =
    ZIO.service[UserRepository] flatMap (_.create(item))
  def create(items: List[User]): ZIO[UserRepository, RepositoryError, List[User]] =
    ZIO.service[UserRepository] flatMap (_.create(items))
  def create2(item: User): ZIO[UserRepository, RepositoryError, Unit]                          =
    ZIO.service[UserRepository] flatMap (_.create2(item))
  def create2(items: List[User]): ZIO[UserRepository, RepositoryError, Int]                    =
    ZIO.service[UserRepository] flatMap (_.create2(items))
  def delete(id: Int, company: String): ZIO[UserRepository, RepositoryError, Int]              =
    ZIO.service[UserRepository] flatMap (_.delete(id, company))
  def delete(ids: List[Int], company: String): ZIO[UserRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))

  def all(companyId: String): ZIO[UserRepository, RepositoryError, List[User]] =
    ZIO.service[UserRepository] flatMap (_.all(companyId))

  def list(company: String): ZStream[UserRepository, RepositoryError, User]                        =
    ZStream.service[UserRepository] flatMap (_.list(company))
  def getByUserName(userName: String, company: String): ZIO[UserRepository, RepositoryError, User] =
    ZIO.service[UserRepository] flatMap (_.getByUserName(userName, company))

  def getById(userId: Int, companyId: String): ZIO[UserRepository, RepositoryError, User]      =
    ZIO.service[UserRepository] flatMap (_.getById(userId, companyId))
  def getByModelId(modelid: Int, company: String): ZIO[UserRepository, RepositoryError, User] =
    ZIO.service[UserRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: User): ZIO[UserRepository, RepositoryError, Int]                          =
    ZIO.service[UserRepository] flatMap (_.modify(model))
  def modify(models: List[User]): ZIO[UserRepository, RepositoryError, Int]                   =
    ZIO.service[UserRepository] flatMap (_.modify(models))

}
