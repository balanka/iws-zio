package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Costcenter
import zio._
import zio.stream._

trait CostcenterRepository {

  def create(item: Costcenter): ZIO[Any, RepositoryError, Unit]
  def create(models: List[Costcenter]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))
  def all(companyId: String): ZIO[Any, RepositoryError, List[Costcenter]]
  def list(company: String): ZStream[Any, RepositoryError, Costcenter]
  def getBy(id:(String,  String)): ZIO[Any, RepositoryError, Costcenter]
  def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[Costcenter]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, Costcenter]
  def modify(model: Costcenter): ZIO[Any, RepositoryError, Int]

}

object CostcenterRepository {

  def create(item: Costcenter): ZIO[CostcenterRepository, RepositoryError, Unit]                               =
    ZIO.service[CostcenterRepository] flatMap (_.create(item))
  def create(items: List[Costcenter]): ZIO[CostcenterRepository, RepositoryError, Int]                         =
    ZIO.service[CostcenterRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[CostcenterRepository, RepositoryError, Int]              =
    ZIO.service[CostcenterRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[CostcenterRepository, RepositoryError, List[Int]] =
    ZIO.foreach(items)(delete(_, company))
  def all(companyId: String): ZIO[CostcenterRepository, RepositoryError, List[Costcenter]]                =
    ZIO.service[CostcenterRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[CostcenterRepository, RepositoryError, Costcenter]                   =
    ZStream.service[CostcenterRepository] flatMap (_.list(company))
  def getBy(id:(String, String)): ZIO[CostcenterRepository, RepositoryError, Costcenter]          =
    ZIO.service[CostcenterRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int, String)): ZIO[CostcenterRepository, RepositoryError, List[Costcenter]] =
    ZIO.service[CostcenterRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[CostcenterRepository, RepositoryError, Costcenter] =
    ZStream.service[CostcenterRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: Costcenter): ZIO[CostcenterRepository, RepositoryError, Int]                          =
    ZIO.service[CostcenterRepository] flatMap (_.modify(model))

}
