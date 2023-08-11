package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Supplier
import zio._
import zio.stream._

trait SupplierRepository {
  def create(item: Supplier): ZIO[Any, RepositoryError, Supplier]

  def create(models: List[Supplier]): ZIO[Any, RepositoryError, List[Supplier]]
  def create2(item: Supplier): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Supplier]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[Supplier]]
  def list(company: String): ZStream[Any, RepositoryError, Supplier]
  def getBy(id:(String, String)): ZIO[Any, RepositoryError, Supplier]
  def getByIban(Iban: String, companyId: String): ZIO[Any, RepositoryError, Supplier]
  def getByModelId(modelid: (Int,  String)): ZIO[Any, RepositoryError, List[Supplier]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Supplier]
  def modify(model: Supplier): ZIO[Any, RepositoryError, Int]

}
object SupplierRepository {

  def create(item: Supplier): ZIO[SupplierRepository, RepositoryError, Supplier] =
    ZIO.service[SupplierRepository] flatMap (_.create(item))

  def create(items: List[Supplier]): ZIO[SupplierRepository, RepositoryError, List[Supplier]] =
    ZIO.service[SupplierRepository] flatMap (_.create(items))
  def create2(item: Supplier): ZIO[SupplierRepository, RepositoryError, Unit]                               =
    ZIO.service[SupplierRepository] flatMap (_.create2(item))
  def create2(items: List[Supplier]): ZIO[SupplierRepository, RepositoryError, Int]                         =
    ZIO.service[SupplierRepository] flatMap (_.create2(items))
  def delete(item: String, company: String): ZIO[SupplierRepository, RepositoryError, Int]              =
    ZIO.service[SupplierRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[SupplierRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(company: String): ZIO[SupplierRepository, RepositoryError, List[Supplier]] =
    ZIO.service[SupplierRepository] flatMap (_.all(company))

  def list(company: String): ZStream[SupplierRepository, RepositoryError, Supplier]                   =
    ZStream.service[SupplierRepository] flatMap (_.list(company))
  def getBy(id:(String,  String)): ZIO[SupplierRepository, RepositoryError, Supplier]          =
    ZIO.service[SupplierRepository] flatMap (_.getBy(id))
  def getByIban(Iban: String, companyId: String): ZIO[SupplierRepository, RepositoryError, Supplier]  =
    ZIO.service[SupplierRepository] flatMap (_.getByIban(Iban, companyId))
  def getByModelId(modelid: (Int,  String)): ZIO[SupplierRepository, RepositoryError, List[Supplier]] =
    ZIO.service[SupplierRepository] flatMap (_.getByModelId(modelid))

  def getByModelIdStream(modelid: Int, company: String): ZStream[SupplierRepository, RepositoryError, Supplier] =
    ZStream.service[SupplierRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Supplier): ZIO[SupplierRepository, RepositoryError, Int]                          =
    ZIO.service[SupplierRepository] flatMap (_.modify(model))

}
