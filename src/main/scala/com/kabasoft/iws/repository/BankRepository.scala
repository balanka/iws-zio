package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Bank
import zio._
import zio.stream._

trait BankRepository {
  def create(item: Bank): ZIO[Any, RepositoryError, Unit]
  def create(models: List[Bank]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[Bank]]
  def list(company: String): ZStream[Any, RepositoryError, Bank]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, Bank]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Bank]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Bank]
  def modify(model: Bank): ZIO[Any, RepositoryError, Int]

}

object BankRepository {

  def create(item: Bank): ZIO[BankRepository, RepositoryError, Unit]                               =
    ZIO.service[BankRepository] flatMap (_.create(item))
  def create(items: List[Bank]): ZIO[BankRepository, RepositoryError, Int]                         =
    ZIO.service[BankRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[BankRepository, RepositoryError, Int]              =
    ZIO.service[BankRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BankRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[BankRepository, RepositoryError, List[Bank]]                      =
    ZIO.service[BankRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[BankRepository, RepositoryError, Bank]                        =
    ZStream.service[BankRepository] flatMap (_.list(company))
  def getBy(id: (String,String)): ZIO[BankRepository, RepositoryError, Bank]               =
    ZIO.service[BankRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[BankRepository, RepositoryError, List[Bank]]      =
    ZIO.service[BankRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[BankRepository, RepositoryError, Bank]=
    ZStream.service[BankRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Bank): ZIO[BankRepository, RepositoryError, Int]                               =
    ZIO.service[BankRepository] flatMap (_.modify(model))

}
