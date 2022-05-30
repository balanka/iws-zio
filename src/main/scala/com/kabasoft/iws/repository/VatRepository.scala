package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait VatRepository {
  type TYPE_ = Vat
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

object VatRepository {

  type TYPE_ = Vat
  def create(item: TYPE_): ZIO[VatRepository, RepositoryError, Unit]                               =
    ZIO.serviceWithZIO[VatRepository](_.create(item))
  def create(items: List[TYPE_]): ZIO[VatRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[VatRepository](_.create(items))
  def delete(item: String, company: String): ZIO[VatRepository, RepositoryError, Int]              =
    ZIO.serviceWithZIO[VatRepository](_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[VatRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[VatRepository, RepositoryError, TYPE_]                        =
    ZStream.serviceWithStream[VatRepository](_.list(company))
  def getBy(id: String, company: String): ZIO[VatRepository, RepositoryError, TYPE_]               =
    ZIO.serviceWithZIO[VatRepository](_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[VatRepository, RepositoryError, TYPE_]      =
    ZIO.serviceWithZIO[VatRepository](_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[VatRepository, RepositoryError, Int]                               =
    ZIO.serviceWithZIO[VatRepository](_.modify(model))
  // def update(model: TYPE_, company: String): ZIO[VatRepository, RepositoryError, Int]=
  //   ZIO.serviceWithZIO[VatRepository](_.update(model, company))
  // def findSome(company: String, param: String*): ZStream[VatRepository, RepositoryError, TYPE_]=
  //  ZStream.serviceWithStream[VatRepository](_.findSome(company,param))

}
