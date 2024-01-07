package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait AccountCache {
  def all(Id:(Int,  String)): ZIO[Any, RepositoryError, List[Account]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Account]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Account]]

}
object AccountCache {
  def all(Id:(Int, String)): ZIO[AccountCache, RepositoryError, List[Account]] =
    ZIO.service[AccountCache] flatMap (_.all(Id))

  def getBy(id:(String, String)): ZIO[AccountCache, RepositoryError, Account]=
    ZIO.service[AccountCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[AccountCache, RepositoryError, List[Account]] =
    ZIO.service[AccountCache] flatMap (_.getByModelId(id))

}
