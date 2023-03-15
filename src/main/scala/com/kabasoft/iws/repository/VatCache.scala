package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Vat
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait VatCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Vat]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Vat]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Vat]]

}
object VatCache {
  def all(companyId: String): ZIO[VatCache, RepositoryError, List[Vat]] =
    ZIO.service[VatCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[VatCache, RepositoryError, Vat]=
    ZIO.service[VatCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[VatCache, RepositoryError, List[Vat]] =
    ZIO.service[VatCache] flatMap (_.getByModelId(id))

}
