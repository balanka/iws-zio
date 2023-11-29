package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.SalaryItem
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait SalaryItemCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[SalaryItem]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, SalaryItem]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[SalaryItem]]

}
object SalaryItemCache {
  def all(companyId: String): ZIO[SalaryItemCache, RepositoryError, List[SalaryItem]] =
    ZIO.service[SalaryItemCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[SalaryItemCache, RepositoryError, SalaryItem]=
    ZIO.service[SalaryItemCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[SalaryItemCache, RepositoryError, List[SalaryItem]] =
    ZIO.service[SalaryItemCache] flatMap (_.getByModelId(id))

}
