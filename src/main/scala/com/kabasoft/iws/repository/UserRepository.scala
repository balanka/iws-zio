package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.User
import zio._
trait UserRepository:
  def create(item: User):ZIO[Any, RepositoryError, Int]
  def create(models: List[User]):ZIO[Any, RepositoryError, Int]
  def modify(model: User):ZIO[Any, RepositoryError, Int]
  def modify(models: List[User]):ZIO[Any, RepositoryError, Int]
  def modifyPwd(model: User):ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[User]]
  def getById(Id: (Int, Int, String)): ZIO[Any, RepositoryError, User]
  def getBy(ids: List[Int], modelid: Int, company: String):ZIO[Any, RepositoryError, List[User]]
  def getByUserName(p: (String, Int, String)):ZIO[Any, RepositoryError, User]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]

object UserRepository:
  def create(item: User): ZIO[UserRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[UserRepository](_.create(item))

  def create(models: List[User]): ZIO[UserRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[UserRepository](_.create(models))

  def modify(model: User): ZIO[UserRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[UserRepository](_.modify(model))

  def modify(models: List[User]): ZIO[UserRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[UserRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[UserRepository, RepositoryError, List[User]] =
    ZIO.serviceWithZIO[UserRepository](_.all(Id))

  def getById(Id: (Int, Int, String)): ZIO[UserRepository, RepositoryError, User] =
    ZIO.serviceWithZIO[UserRepository](_.getById(Id))

  def getBy(ids: List[Int], modelid: Int, company: String): ZIO[UserRepository, RepositoryError, List[User]] =
    ZIO.serviceWithZIO[UserRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[UserRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[UserRepository](_.delete(p))

  def getByUserName(p: (String, Int, String)): URIO[UserRepository, User] =
    ZIO.serviceWithZIO[UserRepository](_.getByUserName(p).catchAll( _=>ZIO.succeed(User.dummy)))

  def modifyPwd(model: User): ZIO[UserRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[UserRepository](_.modifyPwd(model))
    
//  def create(item: User): ZIO[UserRepository, RepositoryError, User] =
//    ZIO.service[UserRepository] flatMap (_.create(item))
//  def create(items: List[User]): ZIO[UserRepository, RepositoryError, List[User]] =
//    ZIO.service[UserRepository] flatMap (_.create(items))
//  def create2(item: User): ZIO[UserRepository, RepositoryError, Unit]                          =
//    ZIO.service[UserRepository] flatMap (_.create2(item))
//  def create2(items: List[User]): ZIO[UserRepository, RepositoryError, Int]                    =
//    ZIO.service[UserRepository] flatMap (_.create2(items))
//  def delete(id: Int, company: String): ZIO[UserRepository, RepositoryError, Int]              =
//    ZIO.service[UserRepository] flatMap (_.delete(id, company))
//  def delete(ids: List[Int], company: String): ZIO[UserRepository, RepositoryError, List[Int]] =
//    ZIO.collectAll(ids.map(delete(_, company)))
//
//  def all(Id:(Int, String)): ZIO[UserRepository, RepositoryError, List[User]] =
//    ZIO.service[UserRepository] flatMap (_.all(Id))
//
//  def list(Id:(Int, String)): ZStream[UserRepository, RepositoryError, User]                        =
//    ZStream.service[UserRepository] flatMap (_.list(Id))
//
//
//  def getById(userId: Int, companyId: String): ZIO[UserRepository, RepositoryError, User]      =
//    ZIO.service[UserRepository] flatMap (_.getById(userId, companyId))
//  def getByModelId(modelid: Int, company: String): ZIO[UserRepository, RepositoryError, User] =
//    ZIO.service[UserRepository] flatMap (_.getByModelId(modelid, company))
//  def modify(model: User): ZIO[UserRepository, RepositoryError, Int]                          =
//    ZIO.service[UserRepository] flatMap (_.modify(model))
//  def modify(models: List[User]): ZIO[UserRepository, RepositoryError, Int]                   =
//    ZIO.service[UserRepository] flatMap (_.modify(models))


