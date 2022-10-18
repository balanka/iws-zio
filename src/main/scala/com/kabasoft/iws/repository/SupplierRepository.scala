package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import zio._
import zio.stream._

trait SupplierRepository {
  type TYPE_ = Supplier
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]

}
object SupplierRepository {
  type TYPE_ = Supplier
  def create(item: TYPE_): ZIO[SupplierRepository, RepositoryError, Unit]                               =
    ZIO.service[SupplierRepository] flatMap (_.create(item))
  def create(items: List[TYPE_]): ZIO[SupplierRepository, RepositoryError, Int]                         =
    ZIO.service[SupplierRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[SupplierRepository, RepositoryError, Int]              =
    ZIO.service[SupplierRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[SupplierRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[SupplierRepository, RepositoryError, TYPE_]                        =
    ZStream.service[SupplierRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[SupplierRepository, RepositoryError, TYPE_]               =
    ZIO.service[SupplierRepository] flatMap (_.getBy(id, company))
  def getByIban(Iban: String, companyId: String): ZIO[SupplierRepository, RepositoryError, TYPE_]=
    ZIO.service[SupplierRepository] flatMap (_.getByIban(Iban, companyId))
  def getByModelId(modelid: Int, company: String): ZIO[SupplierRepository, RepositoryError, TYPE_]      =
    ZIO.service[SupplierRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[SupplierRepository, RepositoryError, Int]                               =
    ZIO.service[SupplierRepository] flatMap (_.modify(model))

}

