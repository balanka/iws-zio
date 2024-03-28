package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Fmodule
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait FModuleCache {
  def all(Id:(Int, String, String)): ZIO[Any, RepositoryError, List[Fmodule]]
  def getBy(id:(Int,  String)): ZIO[Any, RepositoryError, Fmodule]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Fmodule]]

}
object FModuleCache {
  def all(Id:(Int, String, String)): ZIO[FModuleCache, RepositoryError, List[Fmodule]] =
    ZIO.service[FModuleCache] flatMap (_.all(Id))

  def getBy(id:(Int, String)): ZIO[FModuleCache, RepositoryError, Fmodule]=
    ZIO.service[FModuleCache] flatMap (_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[FModuleCache, RepositoryError, List[Fmodule]] =
    ZIO.service[FModuleCache] flatMap (_.getByModelId(id))

}
