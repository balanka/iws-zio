package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Fmodule
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait FModuleCache {
  def all(companyId: String): ZIO[Any, RepositoryError, List[Fmodule]]
  def getBy(id:(Int,  String)): ZIO[Any, RepositoryError, Fmodule]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Fmodule]]

}
object FModuleCache {
  def all(companyId: String): ZIO[FModuleCache, RepositoryError, List[Fmodule]] =
    ZIO.service[FModuleCache] flatMap (_.all(companyId))

  def getBy(id:(Int, String)): ZIO[FModuleCache, RepositoryError, Fmodule]=
    ZIO.service[FModuleCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[FModuleCache, RepositoryError, List[Fmodule]] =
    ZIO.service[FModuleCache] flatMap (_.getByModelId(id))

}
