package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankAccount
import zio._
import zio.stream._

trait BankAccountRepository {
  def create(item: BankAccount): ZIO[Any, RepositoryError, BankAccount]
  def create(models: List[BankAccount]): ZIO[Any, RepositoryError, List[BankAccount]]
  def create2(item: BankAccount): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[BankAccount]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[BankAccount]]
  def list(company: String): ZStream[Any, RepositoryError, BankAccount]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, BankAccount]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, BankAccount]
  def modify(model: BankAccount): ZIO[Any, RepositoryError, Int]

}

object BankAccountRepository {
  def create(item: BankAccount): ZIO[BankAccountRepository, RepositoryError, BankAccount] =
    ZIO.service[BankAccountRepository] flatMap (_.create(item))
  def create(items: List[BankAccount]): ZIO[BankAccountRepository, RepositoryError, List[BankAccount]] =
    ZIO.service[BankAccountRepository] flatMap (_.create(items))
  def create2(item: BankAccount): ZIO[BankAccountRepository, RepositoryError, Unit]                               =
    ZIO.service[BankAccountRepository] flatMap (_.create2(item))
  def create2(items: List[BankAccount]): ZIO[BankAccountRepository, RepositoryError, Int]                         =
    ZIO.service[BankAccountRepository] flatMap (_.create2(items))
  def delete(item: String, company: String): ZIO[BankAccountRepository, RepositoryError, Int]              =
    ZIO.service[BankAccountRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BankAccountRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[BankAccountRepository, RepositoryError, List[BankAccount]]               =
    ZIO.service[BankAccountRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[BankAccountRepository, RepositoryError, BankAccount]                        =
    ZStream.service[BankAccountRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[BankAccountRepository, RepositoryError, BankAccount]               =
    ZIO.service[BankAccountRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[BankAccountRepository, RepositoryError, BankAccount]      =
    ZIO.service[BankAccountRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: BankAccount): ZIO[BankAccountRepository, RepositoryError, Int]                               =
    ZIO.service[BankAccountRepository] flatMap (_.modify(model))

}
