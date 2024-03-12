package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Transaction
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait TransactionCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Transaction]]
  def getByTransId(id:(Long,  String)): ZIO[Any, RepositoryError, Transaction]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Transaction]]

}
object TransactionCache {
  def all(companyId: String): ZIO[TransactionCache, RepositoryError, List[Transaction]] =
    ZIO.service[TransactionCache] flatMap (_.all(companyId))

  def getByTransId(id:(Long, String)): ZIO[TransactionCache, RepositoryError, Transaction]=
    ZIO.service[TransactionCache] flatMap (_.getByTransId(id))

  def getByModelId(id:(Int, String)): ZIO[TransactionCache, RepositoryError, List[Transaction]] =
    ZIO.service[TransactionCache] flatMap (_.getByModelId(id))

}
