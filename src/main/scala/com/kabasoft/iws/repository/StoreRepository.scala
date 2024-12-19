package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Store
import zio._
import zio.stream._

trait StoreRepository:
  def create(item: Store):ZIO[Any, RepositoryError, Int]
  def create(models: List[Store]):ZIO[Any, RepositoryError, Int]
  def modify(model: Store):ZIO[Any, RepositoryError, Int]
  def modify(models: List[Store]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)):ZIO[Any, RepositoryError, List[Store]]
  def getById(Id: (String, Int, String)):ZIO[Any, RepositoryError, Store]
  def getBy(ids: List[String], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Store]]
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]

object StoreRepository:
  def create(item: Store): ZIO[StoreRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StoreRepository](_.create(item))
  def create(models: List[Store]): ZIO[StoreRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StoreRepository](_.create(models))
  def modify(model: Store): ZIO[StoreRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StoreRepository](_.modify(model))
  def modify(models: List[Store]): ZIO[StoreRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StoreRepository](_.modify(models))
  def all(Id: (Int, String)): ZIO[StoreRepository, RepositoryError, List[Store]] =
    ZIO.serviceWithZIO[StoreRepository](_.all(Id))
  def getById(Id: (String, Int, String)): ZIO[StoreRepository, RepositoryError, Store] =
    ZIO.serviceWithZIO[StoreRepository](_.getById(Id))
  def getBy(ids: List[String], modelid: Int, company: String): ZIO[StoreRepository, RepositoryError, List[Store]] =
    ZIO.serviceWithZIO[StoreRepository](_.getBy(ids, modelid, company))
  def delete(p: (String, Int, String)): ZIO[StoreRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[StoreRepository](_.delete(p))

