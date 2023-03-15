package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Customer
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait CustomerCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Customer]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Customer]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Customer]]

}
object CustomerCache {
  def all(companyId: String): ZIO[CustomerCache, RepositoryError, List[Customer]] =
    ZIO.service[CustomerCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[CustomerCache, RepositoryError, Customer]=
    ZIO.service[CustomerCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[CustomerCache, RepositoryError, List[Customer]] =
    ZIO.service[CustomerCache] flatMap (_.getByModelId(id))

}
