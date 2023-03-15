package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Supplier
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait SupplierCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Supplier]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Supplier]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Supplier]]

}
object SupplierCache {
  def all(companyId: String): ZIO[SupplierCache, RepositoryError, List[Supplier]] =
    ZIO.service[SupplierCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[SupplierCache, RepositoryError, Supplier]=
    ZIO.service[SupplierCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[SupplierCache, RepositoryError, List[Supplier]] =
    ZIO.service[SupplierCache] flatMap (_.getByModelId(id))

}