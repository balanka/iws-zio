package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._
import zio.stream._

trait AccountRepository {
  type TYPE_ = Account
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]
  def modify(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
}
object AccountRepository {

  type TYPE_ = Account

  def create(item: TYPE_): ZIO[AccountRepository, RepositoryError, Unit] =
    ZIO.serviceWithZIO[AccountRepository](_.create(item))

  def create(items: List[TYPE_]): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.create(items))

  def delete(item: String, company: String): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.delete(item, company))

  def delete(items: List[String], company: String): ZIO[AccountRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def list(company: String): ZStream[AccountRepository, RepositoryError, TYPE_] =
    ZStream.serviceWithStream[AccountRepository](_.list(company))

  def getBy(id: String, company: String): ZIO[AccountRepository, RepositoryError, TYPE_] =
    ZIO.serviceWithZIO[AccountRepository](_.getBy(id, company))

  def getByModelId(modelid: Int, company: String): ZIO[AccountRepository, RepositoryError, TYPE_] =
    ZIO.serviceWithZIO[AccountRepository](_.getByModelId(modelid, company))

  def modify(model: TYPE_): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.modify(model))

  def modify(models: List[TYPE_]): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountRepository](_.modify(models))
}

