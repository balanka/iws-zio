package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Module
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait ModuleCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Module]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Module]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Module]]

}
object ModuleCache {
  def all(companyId: String): ZIO[ModuleCache, RepositoryError, List[Module]] =
    ZIO.service[ModuleCache] flatMap (_.all(companyId))

  def getBy(id:(String, String)): ZIO[ModuleCache, RepositoryError, Module]=
    ZIO.service[ModuleCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[ModuleCache, RepositoryError, List[Module]] =
    ZIO.service[ModuleCache] flatMap (_.getByModelId(id))

}
