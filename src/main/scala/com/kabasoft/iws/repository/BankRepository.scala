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
  // def update(model:TYPE_, company: String): ZIO[Any, RepositoryError, Int]
  // def findSome(company: String, param: Seq[String]): ZStream[Any, RepositoryError, TYPE_]
}

object BankRepository {

  type TYPE_ = Bank
  def create(item: TYPE_): ZIO[BankRepository, RepositoryError, Unit]                               =
    ZIO.serviceWithZIO[BankRepository](_.create(item))
  def create(items: List[TYPE_]): ZIO[BankRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[BankRepository](_.create(items))
  def delete(item: String, company: String): ZIO[BankRepository, RepositoryError, Int]              =
    ZIO.serviceWithZIO[BankRepository](_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BankRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[BankRepository, RepositoryError, TYPE_]                        =
    ZStream.serviceWithStream[BankRepository](_.list(company))
  def getBy(id: String, company: String): ZIO[BankRepository, RepositoryError, TYPE_]               =
    ZIO.serviceWithZIO[BankRepository](_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[BankRepository, RepositoryError, TYPE_]      =
    ZIO.serviceWithZIO[BankRepository](_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[BankRepository, RepositoryError, Int]                               =
    ZIO.serviceWithZIO[BankRepository](_.modify(model))

  // def update(models: List[TYPE_], company: String): ZIO[BankRepository, RepositoryError, Int]=
  //  ZIO.collectAll(models.map(update(_, company)))
  // def update(model: TYPE_, company: String): ZIO[BankRepository, RepositoryError, Int]=
  //   ZIO.serviceWithZIO[BankRepository](_.update(model, company))
  // def findSome(company: String, param: String*): ZStream[BankRepository, RepositoryError, TYPE_]=
  //  ZStream.serviceWithStream[BankRepository](_.findSome(company,param))

}
