package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.PayrollTaxRange
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait PayrollTaxRangeCache {
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]]
  def getBy(id:(String, Int,  String)): ZIO[Any, RepositoryError, PayrollTaxRange]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]]

}
object PayrollTaxRangeCache {
  def all(Id:(Int, String)): ZIO[PayrollTaxRangeCache, RepositoryError, List[PayrollTaxRange]] =
    ZIO.service[PayrollTaxRangeCache] flatMap (_.all(Id))

  def getBy(id:(String, Int, String)): ZIO[PayrollTaxRangeCache, RepositoryError, PayrollTaxRange]=
    ZIO.service[PayrollTaxRangeCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[PayrollTaxRangeCache, RepositoryError, List[PayrollTaxRange]] =
    ZIO.service[PayrollTaxRangeCache] flatMap (_.getByModelId(id))

}
