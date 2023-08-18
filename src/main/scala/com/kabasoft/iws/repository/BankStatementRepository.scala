package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankStatement
import zio._
import zio.stream._

trait BankStatementRepository {
  type BS = BankStatement

  def create(item: BS): ZIO[Any, RepositoryError, BS]

  def create(models: List[BS]): ZIO[Any, RepositoryError, List[BS]]
  def create2(item: BS): ZIO[Any, RepositoryError, Unit]
  def create2(models: List[BS]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(companyId: String): ZIO[Any, RepositoryError, List[BankStatement]]
  def list(company: String): ZStream[Any, RepositoryError, BankStatement]
  def listByIds(ids: List[Long], company: String): ZStream[Any, RepositoryError, BankStatement] =
    list(company).filter(bs => ids.contains(bs.id))
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, BankStatement]

  def getById(id: Long): ZIO[Any, RepositoryError, BankStatement]
  def getById(id: List[Long]): ZStream[Any, RepositoryError, BankStatement]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, BankStatement]
  def modify(model: BS): ZIO[Any, RepositoryError, Int]
  def update(model: BankStatement): ZIO[Any, RepositoryError, BankStatement]

}

object BankStatementRepository {
  type BSRepository = BankStatementRepository
  type BS           = BankStatement

  def create(item: BS): ZIO[BSRepository, RepositoryError, BS] =
    ZIO.service[BSRepository] flatMap (_.create(item))

  def create(items: List[BS]): ZIO[BSRepository, RepositoryError, List[BS]] =
    ZIO.service[BSRepository] flatMap (_.create(items))
  def create2(item: BS): ZIO[BSRepository, RepositoryError, Unit]                                  =
    ZIO.service[BSRepository] flatMap (_.create2(item))
  def create2(items: List[BS]): ZIO[BSRepository, RepositoryError, Int]                            =
    ZIO.service[BSRepository] flatMap (_.create2(items))
  def delete(item: String, company: String): ZIO[BSRepository, RepositoryError, Int]              =
    ZIO.service[BSRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BSRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))

  def all(companyId: String): ZIO[BSRepository, RepositoryError, List[BankStatement]] =
    ZIO.service[BSRepository] flatMap (_.all(companyId))

  def list(company: String): ZStream[BSRepository, RepositoryError, BankStatement]                   =
    ZStream.service[BSRepository] flatMap (_.list(company))

  def getById(id: Long): ZIO[BSRepository, RepositoryError, BankStatement]          =
    ZIO.service[BSRepository] flatMap (_.getById(id))

  def getById(id: List[Long]): ZStream[BSRepository, RepositoryError, BankStatement] =
    ZStream.service[BSRepository] flatMap (_.getById(id))

  def getBy(id: String, company: String): ZIO[BSRepository, RepositoryError, BankStatement] =
    ZIO.service[BSRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[BSRepository, RepositoryError, BankStatement] =
    ZIO.service[BSRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: BS): ZIO[BSRepository, RepositoryError, Int]                                     =
    ZIO.service[BSRepository] flatMap (_.modify(model))

  def update(model: BS): ZIO[BSRepository, RepositoryError, BS] =
    ZIO.service[BSRepository] flatMap (_.update(model))
}
