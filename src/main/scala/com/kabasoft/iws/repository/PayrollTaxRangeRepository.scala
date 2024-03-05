package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PayrollTaxRange
import zio._
import zio.stream._

trait PayrollTaxRangeRepository {
  def create(item: PayrollTaxRange): ZIO[Any, RepositoryError, PayrollTaxRange]
  def create(models: List[PayrollTaxRange]): ZIO[Any, RepositoryError, List[PayrollTaxRange]]
  def create2(item: PayrollTaxRange): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[PayrollTaxRange]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, modelId:Int, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], modelId:Int, company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, modelId, company)))
  def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]]
  def list(Id:(Int, String)): ZStream[Any, RepositoryError, PayrollTaxRange]
  def getBy(id: (String, Int,  String)): ZIO[Any, RepositoryError, PayrollTaxRange]
  def getByModelId(modelid:(Int, String)): ZIO[Any, RepositoryError, List[PayrollTaxRange]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, PayrollTaxRange]
  def modify(model: PayrollTaxRange): ZIO[Any, RepositoryError, Int]

}

object PayrollTaxRangeRepository {
  def create(item: PayrollTaxRange): ZIO[PayrollTaxRangeRepository, RepositoryError, PayrollTaxRange] =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.create(item))

  def create(items: List[PayrollTaxRange]): ZIO[PayrollTaxRangeRepository, RepositoryError, List[PayrollTaxRange]] =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.create(items))
  def create2(item: PayrollTaxRange): ZIO[PayrollTaxRangeRepository, RepositoryError, Unit]                               =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.create2(item))
  def create2(items: List[PayrollTaxRange]): ZIO[PayrollTaxRangeRepository, RepositoryError, Int]                         =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.create2(items))
  def delete(item: String, modelId:Int, company: String): ZIO[PayrollTaxRangeRepository, RepositoryError, Int]              =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.delete(item, modelId, company))
  def delete(items: List[String], modelId:Int, company: String): ZIO[PayrollTaxRangeRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, modelId, company)))

  def all(Id:(Int, String)): ZIO[PayrollTaxRangeRepository, RepositoryError, List[PayrollTaxRange]]                =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.all(Id))
  def list(Id:(Int, String)): ZStream[PayrollTaxRangeRepository, RepositoryError, PayrollTaxRange]                   =
    ZStream.service[PayrollTaxRangeRepository] flatMap (_.list(Id))
  def getBy(id:(String,Int,  String)): ZIO[PayrollTaxRangeRepository, RepositoryError, PayrollTaxRange]          =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.getBy(id))
  def getByModelId(modelid:(Int,  String)): ZIO[PayrollTaxRangeRepository, RepositoryError, List[PayrollTaxRange]] =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[PayrollTaxRangeRepository, RepositoryError, PayrollTaxRange] =
    ZStream.service[PayrollTaxRangeRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: PayrollTaxRange): ZIO[PayrollTaxRangeRepository, RepositoryError, Int]                          =
    ZIO.service[PayrollTaxRangeRepository] flatMap (_.modify(model))

}
