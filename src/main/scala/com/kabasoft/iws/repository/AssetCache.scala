package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Asset
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._


trait AssetCache {
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Asset]]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Asset]
  def getByModelId(id:(Int, String)): ZIO[Any, RepositoryError, List[Asset]]

}
object AssetCache {
  def all(Id:(Int, String)): ZIO[AssetCache, RepositoryError, List[Asset]] =
    ZIO.serviceWithZIO[AssetCache](_.all(Id))

  def getBy(id:(String, String)): ZIO[AssetCache, RepositoryError, Asset]=
    ZIO.serviceWithZIO[AssetCache](_.getBy(id))

  def getByModelId(id:(Int, String)): ZIO[AssetCache, RepositoryError, List[Asset]] =
    ZIO.serviceWithZIO[AssetCache](_.getByModelId(id))

}
