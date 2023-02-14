package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Costcenter
import zio._
import zio.stream._

trait CostcenterRepository {
  type TYPE_ = Costcenter
  def create(item: TYPE_): ZIO[Any, RepositoryError, Unit]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))
  def all(companyId: String): ZIO[Any, RepositoryError, List[TYPE_]]
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, TYPE_]
  def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]

}

object CostcenterRepository {

  type TYPE_ = Costcenter
  def create(item: TYPE_): ZIO[CostcenterRepository, RepositoryError, Unit]                               =
    ZIO.service[CostcenterRepository] flatMap (_.create(item))
  def create(items: List[TYPE_]): ZIO[CostcenterRepository, RepositoryError, Int]                         =
    ZIO.service[CostcenterRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[CostcenterRepository, RepositoryError, Int]              =
    ZIO.service[CostcenterRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[CostcenterRepository, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))

  def all(companyId: String): ZIO[CostcenterRepository, RepositoryError, List[TYPE_]] =
    ZIO.service[CostcenterRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[CostcenterRepository, RepositoryError, TYPE_]                        =
    ZStream.service[CostcenterRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[CostcenterRepository, RepositoryError, TYPE_]               =
    ZIO.service[CostcenterRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[CostcenterRepository, RepositoryError, TYPE_]      =
    ZIO.service[CostcenterRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: TYPE_): ZIO[CostcenterRepository, RepositoryError, Int]                               =
    ZIO.service[CostcenterRepository] flatMap (_.modify(model))

}
