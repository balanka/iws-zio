package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait BankCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Bank]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Bank]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Bank]]

}
object BankCache {
  def all(companyId: String): ZIO[BankCache, RepositoryError, List[Bank]] =
    ZIO.service[BankCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[BankCache, RepositoryError, Bank]=
    ZIO.service[BankCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[BankCache, RepositoryError, List[Bank]] =
    ZIO.service[BankCache] flatMap (_.getByModelId(id))

}
