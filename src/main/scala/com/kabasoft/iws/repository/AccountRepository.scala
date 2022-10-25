package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._
import zio.stream._

trait AccountRepository  {
  def create(item: Account): ZIO[Any, RepositoryError, Unit]
  def create(models: List[Account]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, Account]
  def all(companyId: String): ZStream[Any, RepositoryError, Account]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, Account]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, Account]
  def modify(model: Account): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Account]): ZIO[Any, RepositoryError, Int]
}
object AccountRepository {

  def create(item: Account): ZIO[AccountRepository, RepositoryError, Unit] =
    ZIO.service[AccountRepository] flatMap (_.create(item))

  def create(items: List[Account]): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.service[AccountRepository] flatMap (_.create(items))

  def delete(item: String, company: String): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.service[AccountRepository] flatMap (_.delete(item, company))

  def delete(items: List[String], company: String): ZIO[AccountRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def list(company: String): ZStream[AccountRepository, RepositoryError, Account] =
    ZStream.service[AccountRepository] flatMap (_.list(company))

  def all(companyId: String): ZStream[AccountRepository, RepositoryError, Account]=
    ZStream.service[AccountRepository] flatMap (_.all(companyId))

  def getBy(id: String, company: String): ZIO[AccountRepository, RepositoryError, Account] =
    ZIO.service[AccountRepository] flatMap (_.getBy(id, company))

  def getByModelId(modelid: Int, company: String): ZIO[AccountRepository, RepositoryError, Account] =
    ZIO.service[AccountRepository] flatMap (_.getByModelId(modelid, company))

  def modify(model: Account): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.service[AccountRepository] flatMap (_.modify(model))

  def modify(models: List[Account]): ZIO[AccountRepository, RepositoryError, Int] =
    ZIO.service[AccountRepository] flatMap (_.modify(models))
}
