
package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Bom
import zio._
import zio.stream._

trait BomRepository {
  def create(models: List[Bom]): ZIO[Any, RepositoryError, Int]
  def all(companyId: String): ZIO[Any, RepositoryError, List[Bom]]
  def list(company: String): ZStream[Any, RepositoryError, Bom]
  def getBy(id: String, company: String): IO[RepositoryError, Bom]
  def getByIds(ids: List[String], company: String): IO[RepositoryError, List[Bom]]
  def modify(models: List[Bom]): ZIO[Any, RepositoryError, Int]
}

object BomRepository {
  def create(items: List[Bom]): ZIO[BomRepository, RepositoryError, Int]                                                =
    ZIO.serviceWithZIO[BomRepository](_.create(items))
  def all(companyId: String): ZIO[BomRepository, RepositoryError, List[Bom]]                                            =
    ZIO.serviceWithZIO[BomRepository](_.all(companyId))
  def list(company: String): ZStream[BomRepository, RepositoryError, Bom]                                               =
    ZStream.service[BomRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[BomRepository, RepositoryError, Bom]                                      =
    ZIO.serviceWithZIO[BomRepository](_.getBy(id, company))
  def getByIds(ids: List[String], companyId: String): ZIO[BomRepository, RepositoryError, List[Bom]]                    =
    ZIO.serviceWithZIO[BomRepository](_.getByIds(ids, companyId))
  def modify(models: List[Bom]): ZIO[BomRepository, RepositoryError, Int]                                               =
    ZIO.serviceWithZIO[BomRepository](_.modify(models))
}
