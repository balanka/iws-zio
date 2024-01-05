package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Asset
import zio._
import zio.stream._

trait AssetRepository {
  def create(item: Asset): ZIO[Any, RepositoryError, Asset]

  def create(models: List[Asset]): ZIO[Any, RepositoryError, List[Asset]]
  def create2(item: Asset): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[Asset]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Asset]]
  def list(Id:(Int, String)): ZStream[Any, RepositoryError, Asset]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, Asset]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[Asset]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Asset]
  def modify(model: Asset): ZIO[Any, RepositoryError, Int]

}

object AssetRepository {
  def create(item: Asset): ZIO[AssetRepository, RepositoryError, Asset] =
    ZIO.serviceWithZIO[AssetRepository](_.create(item))

  def create(items: List[Asset]): ZIO[AssetRepository, RepositoryError, List[Asset]] =
    ZIO.serviceWithZIO[AssetRepository](_.create(items))
  def create2(item: Asset): ZIO[AssetRepository, RepositoryError, Unit]                               =
    ZIO.serviceWithZIO[AssetRepository](_.create2(item))
  def create2(items: List[Asset]): ZIO[AssetRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[AssetRepository](_.create2(items))
  def delete(item: String, company: String): ZIO[AssetRepository, RepositoryError, Int]              =
    ZIO.serviceWithZIO[AssetRepository](_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[AssetRepository, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))
  def all(Id:(Int, String)): ZIO[AssetRepository, RepositoryError, List[Asset]]                      =
    ZIO.serviceWithZIO[AssetRepository](_.all(Id))
  def list(Id:(Int, String)): ZStream[AssetRepository, RepositoryError, Asset]                        =
    ZStream.service[AssetRepository] flatMap (_.list(Id))
  def getBy(id: (String,String)): ZIO[AssetRepository, RepositoryError, Asset]               =
    ZIO.serviceWithZIO[AssetRepository](_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[AssetRepository, RepositoryError, List[Asset]]      =
    ZIO.serviceWithZIO[AssetRepository](_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[AssetRepository, RepositoryError, Asset]=
    ZStream.service[AssetRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Asset): ZIO[AssetRepository, RepositoryError, Int]                               =
    ZIO.serviceWithZIO[AssetRepository](_.modify(model))

}
