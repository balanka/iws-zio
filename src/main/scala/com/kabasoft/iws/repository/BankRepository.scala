package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait BankRepository {
  type TYPE_ = Bank
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]

}

object BankRepository {

  type TYPE_ = Bank
  def create(item: TYPE_): ZIO[BankRepository, RepositoryError, Unit]                               =
    ZIO.service[BankRepository] flatMap (_.create(item))
  def create(items: List[TYPE_]): ZIO[BankRepository, RepositoryError, Int]                         =
    ZIO.service[BankRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[BankRepository, RepositoryError, Int]              =
    ZIO.service[BankRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BankRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[BankRepository, RepositoryError, TYPE_]                        =
    ZStream.service[BankRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[BankRepository, RepositoryError, TYPE_]               =
    ZIO.service[BankRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[BankRepository, RepositoryError, TYPE_]      =
    ZIO.service[BankRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[BankRepository, RepositoryError, Int]                               =
    ZIO.service[BankRepository] flatMap (_.modify(model))

}
