package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait UserRepository {
  type TYPE_ = User
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getByUserName(userName: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]
  def modify(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
}

object UserRepository {

  type TYPE_ = User
  def create(item: TYPE_): ZIO[UserRepository, RepositoryError, Unit]                               =
    ZIO.service[UserRepository]flatMap(_.create(item))
  def create(items: List[TYPE_]): ZIO[UserRepository, RepositoryError, Int]                         =
    ZIO.service[UserRepository]flatMap(_.create(items))
  def delete(item: String, company: String): ZIO[UserRepository, RepositoryError, Int]              =
    ZIO.service[UserRepository]flatMap(_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[UserRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[UserRepository, RepositoryError, TYPE_]                        =
    ZStream.service[UserRepository]flatMap(_.list(company))
  def getByUserName(userName: String, company: String): ZIO[UserRepository, RepositoryError, TYPE_]               =
    ZIO.service[UserRepository]flatMap(_.getByUserName(userName, company))
  def getByModelId(modelid: Int, company: String): ZIO[UserRepository, RepositoryError, TYPE_]      =
    ZIO.service[UserRepository]flatMap(_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[UserRepository, RepositoryError, Int]                               =
    ZIO.service[UserRepository]flatMap(_.modify(model))
  def modify(models: List[TYPE_]): ZIO[UserRepository, RepositoryError, Int]                        =
    ZIO.service[UserRepository]flatMap(_.modify(models))

}
