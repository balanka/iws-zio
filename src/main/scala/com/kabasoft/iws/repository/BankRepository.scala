package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.{AppError, Bank}
import zio._
import zio.stream._

trait BankRepository {
  def create(item: Bank): ZIO[Any, AppError, Unit]
  def create(models: List[Bank]): ZIO[Any, AppError, Int]
  def delete(item: String, company: String): ZIO[Any, AppError, Int]
  def delete(items: List[String], company: String): ZIO[Any, AppError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, AppError, List[Bank]]
  def list(company: String): ZStream[Any, AppError, Bank]
  def getBy(id: String, company: String): ZIO[Any, AppError, Bank]
  def getByModelId(modelid: Int, company: String): ZIO[Any, AppError, Bank]
  def modify(model: Bank): ZIO[Any, AppError, Int]

}

object BankRepository {

  def create(item: Bank): ZIO[BankRepository, AppError, Unit]                               =
    ZIO.service[BankRepository] flatMap (_.create(item))
  def create(items: List[Bank]): ZIO[BankRepository, AppError, Int]                         =
    ZIO.service[BankRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[BankRepository, AppError, Int]              =
    ZIO.service[BankRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BankRepository, AppError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[BankRepository, AppError, List[Bank]]                      =
    ZIO.service[BankRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[BankRepository, AppError, Bank]                        =
    ZStream.service[BankRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[BankRepository, AppError, Bank]               =
    ZIO.service[BankRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[BankRepository, AppError, Bank]      =
    ZIO.service[BankRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: Bank): ZIO[BankRepository, AppError, Int]                               =
    ZIO.service[BankRepository] flatMap (_.modify(model))

}
