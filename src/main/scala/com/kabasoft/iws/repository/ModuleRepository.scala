package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait ModuleRepository {
  type TYPE_ = Module
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]

}

object ModuleRepository {

  type TYPE_ = Module
  def create(item: TYPE_): ZIO[ModuleRepository, RepositoryError, Unit]                               =
    ZIO.service[ModuleRepository] flatMap (_.create(item))
  def create(items: List[TYPE_]): ZIO[ModuleRepository, RepositoryError, Int]                         =
    ZIO.service[ModuleRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[ModuleRepository, RepositoryError, Int]              =
    ZIO.service[ModuleRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[ModuleRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[ModuleRepository, RepositoryError, TYPE_]                        =
    ZStream.service[ModuleRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[ModuleRepository, RepositoryError, TYPE_]               =
    ZIO.service[ModuleRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[ModuleRepository, RepositoryError, TYPE_]      =
    ZIO.service[ModuleRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[ModuleRepository, RepositoryError, Int]                               =
    ZIO.service[ModuleRepository] flatMap (_.modify(model))

}
