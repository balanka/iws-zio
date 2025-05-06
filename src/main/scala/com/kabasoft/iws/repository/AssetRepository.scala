package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Asset
import zio._

trait AssetRepository:
  def create(item: Asset): ZIO[Any, RepositoryError, Int]
  def create(models: List[Asset]): ZIO[Any, RepositoryError, Int]
  def modify(model: Asset): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Asset]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Asset]]
  def getById(Id: (String, Int, String)): ZIO[Any, RepositoryError, Asset]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Asset]]
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int]

object AssetRepository:

  def create(item: Asset): ZIO[AssetRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AssetRepository](_.create(item))

  def create(models: List[Asset]): ZIO[AssetRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AssetRepository](_.create(models))

  def modify(model: Asset): ZIO[AssetRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AssetRepository](_.modify(model))

  def modify(models: List[Asset]): ZIO[AssetRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AssetRepository](_.modify(models))

  def all(Id: (Int, String)): ZIO[AssetRepository, RepositoryError, List[Asset]] =
    ZIO.serviceWithZIO[AssetRepository](_.all(Id))

  def getById(Id: (String, Int, String)): ZIO[AssetRepository, RepositoryError, Asset] =
    ZIO.serviceWithZIO[AssetRepository](_.getById(Id))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[AssetRepository, RepositoryError, List[Asset]] =
    ZIO.serviceWithZIO[AssetRepository](_.getBy(ids, modelid, company))

  def delete(p: (String, Int, String)): ZIO[AssetRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[AssetRepository](_.delete(p))