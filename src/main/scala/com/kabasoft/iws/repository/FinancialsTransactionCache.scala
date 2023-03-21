package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.FinancialsTransaction
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait FinancialsTransactionCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getByTransId(id:(Long,  String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]

}
object FinancialsTransactionCache {
  def all(companyId: String): ZIO[FinancialsTransactionCache, RepositoryError, List[FinancialsTransaction]] =
    ZIO.service[FinancialsTransactionCache] flatMap (_.all(companyId))

  def getByTransId(id:(Long, String)): ZIO[FinancialsTransactionCache, RepositoryError, FinancialsTransaction]=
    ZIO.service[FinancialsTransactionCache] flatMap (_.getByTransId(id))

  def getByModelId(id:(Int, String)): ZIO[FinancialsTransactionCache, RepositoryError, List[FinancialsTransaction]] =
    ZIO.service[FinancialsTransactionCache] flatMap (_.getByModelId(id))

}
