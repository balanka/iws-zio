package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Vat
import zio._
import zio.stream._

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
  def modify(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
}

object VatRepository {

  type TYPE_ = Vat
  def create(item: TYPE_): ZIO[VatRepository, RepositoryError, Unit]                               =
    ZIO.service[VatRepository]flatMap(_.create(item))
  def create(items: List[TYPE_]): ZIO[VatRepository, RepositoryError, Int]                         =
    ZIO.service[VatRepository]flatMap(_.create(items))
  def delete(item: String, company: String): ZIO[VatRepository, RepositoryError, Int]              =
    ZIO.service[VatRepository]flatMap(_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[VatRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[VatRepository, RepositoryError, TYPE_]                        =
    ZStream.service[VatRepository]flatMap(_.list(company))
  def getBy(id: String, company: String): ZIO[VatRepository, RepositoryError, TYPE_]               =
    ZIO.service[VatRepository]flatMap(_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[VatRepository, RepositoryError, TYPE_]      =
    ZIO.service[VatRepository]flatMap(_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[VatRepository, RepositoryError, Int]                               =
    ZIO.service[VatRepository]flatMap(_.modify(model))
  def modify(models: List[TYPE_]): ZIO[VatRepository, RepositoryError, Int]                        =
    ZIO.service[VatRepository]flatMap(_.modify(models))

}
