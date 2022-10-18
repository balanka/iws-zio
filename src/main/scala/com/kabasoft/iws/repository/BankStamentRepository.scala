package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait BankStatementRepository {
  type BS = BankStatement
  def create(item: BS): ZIO[Any, RepositoryError, Unit]
  def create(models: List[BS]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]]          =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, BankStatement]
  def listByIds(ids: List[Long], company: String): ZStream[Any, RepositoryError, BankStatement] =
    list(company).filter(bs => ids.contains(bs.id))
   // ZIO.collectAll(ids.map(id => getBy(id.toString, company)))
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, BankStatement]
  def getByModelId(modelid: Int, company: String): ZIO[Any, RepositoryError, BankStatement]
  def modify(model: BS): ZIO[Any, RepositoryError, Int]

}

object BankStatementRepository {
  type BSRepository = BankStatementRepository
  type BS           = BankStatement
  def create(item: BS): ZIO[BSRepository, RepositoryError, Unit]                                           =
    ZIO.service[BSRepository] flatMap (_.create(item))
  def create(items: List[BS]): ZIO[BSRepository, RepositoryError, Int]                                     =
    ZIO.service[BSRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[BSRepository, RepositoryError, Int]                       =
    ZIO.service[BSRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[BSRepository, RepositoryError, List[Int]]          =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[BSRepository, RepositoryError, BankStatement]                         =
    ZStream.service[BSRepository] flatMap (_.list(company))
 // def listByIds(ids: List[Long], company: String): ZIO[BSRepository, RepositoryError, List[BankStatement]] =
  //  ZIO.service[BSRepository] flatMap (_.listByIds(ids, company))
  def getBy(id: String, company: String): ZIO[BSRepository, RepositoryError, BankStatement]                =
    ZIO.service[BSRepository] flatMap (_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[BSRepository, RepositoryError, BankStatement]       =
    ZIO.service[BSRepository] flatMap (_.getByModelId(modelid, company))
  def modify(model: BS): ZIO[BSRepository, RepositoryError, Int]                                           =
    ZIO.service[BSRepository] flatMap (_.modify(model))

}
