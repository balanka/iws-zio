package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.SalaryItem
import zio._
import zio.stream._

trait SalaryItemRepository {
  def create(item: SalaryItem): ZIO[Any, RepositoryError, SalaryItem]

  def create(models: List[SalaryItem]): ZIO[Any, RepositoryError, List[SalaryItem]]
  def create2(item: SalaryItem): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[SalaryItem]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[SalaryItem]]
  def list(company: String): ZStream[Any, RepositoryError, SalaryItem]
  def getBy(id: (String,String)): ZIO[Any, RepositoryError, SalaryItem]
  def getByModelId(modelid:(Int,String)): ZIO[Any, RepositoryError, List[SalaryItem]]
  def getByModelIdStream(modelid: Int, company: String): ZStream[Any, RepositoryError, SalaryItem]
  def modify(model: SalaryItem): ZIO[Any, RepositoryError, Int]

}

object SalaryItemRepository {
  def create(item: SalaryItem): ZIO[SalaryItemRepository, RepositoryError, SalaryItem] =
    ZIO.service[SalaryItemRepository] flatMap (_.create(item))

  def create(items: List[SalaryItem]): ZIO[SalaryItemRepository, RepositoryError, List[SalaryItem]] =
    ZIO.service[SalaryItemRepository] flatMap (_.create(items))
  def create2(item: SalaryItem): ZIO[SalaryItemRepository, RepositoryError, Unit]                               =
    ZIO.service[SalaryItemRepository] flatMap (_.create2(item))
  def create2(items: List[SalaryItem]): ZIO[SalaryItemRepository, RepositoryError, Int]                         =
    ZIO.service[SalaryItemRepository] flatMap (_.create2(items))
  def delete(item: String, company: String): ZIO[SalaryItemRepository, RepositoryError, Int]              =
    ZIO.service[SalaryItemRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[SalaryItemRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[SalaryItemRepository, RepositoryError, List[SalaryItem]]                      =
    ZIO.service[SalaryItemRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[SalaryItemRepository, RepositoryError, SalaryItem]                        =
    ZStream.service[SalaryItemRepository] flatMap (_.list(company))
  def getBy(id: (String,String)): ZIO[SalaryItemRepository, RepositoryError, SalaryItem]               =
    ZIO.service[SalaryItemRepository] flatMap (_.getBy(id))
  def getByModelId(modelid: (Int,String)): ZIO[SalaryItemRepository, RepositoryError, List[SalaryItem]]      =
    ZIO.service[SalaryItemRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdStream(modelid: Int, company: String): ZStream[SalaryItemRepository, RepositoryError, SalaryItem]=
    ZStream.service[SalaryItemRepository] flatMap (_.getByModelIdStream(modelid, company))
  def modify(model: SalaryItem): ZIO[SalaryItemRepository, RepositoryError, Int]                               =
    ZIO.service[SalaryItemRepository] flatMap (_.modify(model))

}
