package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankAccount
import zio._
import zio.stream._

trait BankAccountRepository {
  type TYPE_ = BankAccount
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[BankAccount]]
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]

}

object BankAccountRepository {

  type TYPE_ = BankAccount
  def create(item: TYPE_): ZIO[BankAccountRepository, RepositoryError, Unit]                               =
    ZIO.service[BankAccountRepository] flatMap (_.create(item))
  def create(items: List[TYPE_]): ZIO[BankAccountRepository, RepositoryError, Int]                         =
    ZIO.service[BankAccountRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[BankAccountRepository, RepositoryError, Int]              =
    ZIO.service[BankAccountRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BankAccountRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[BankAccountRepository, RepositoryError, List[BankAccount]]  =
    ZIO.service[BankAccountRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[BankAccountRepository, RepositoryError, TYPE_]                        =
    ZStream.service[BankAccountRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[BankAccountRepository, RepositoryError, TYPE_]               =
    ZIO.service[BankAccountRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[BankAccountRepository, RepositoryError, TYPE_]      =
    ZIO.service[BankAccountRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[BankAccountRepository, RepositoryError, Int]                               =
    ZIO.service[BankAccountRepository] flatMap (_.modify(model))

}
