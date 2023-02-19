package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{ DerivedTransaction, FinancialsTransaction }
import zio._
import zio.stream._

trait TransactionRepository {
  type TYPE_ = DerivedTransaction
  def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def create(item: TYPE_): ZIO[Any, RepositoryError, Int]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def create2(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]   =
    ZIO.collectAll(models.map(create(_))).map(_.sum)
  def delete(item: Long, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(ids.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def all(companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByTransId(id: Long, companyId: String): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getByModelId(modelid: Int, company: String): ZStream[Any, RepositoryError, TYPE_]
  def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, FinancialsTransaction]
  def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def modify(model: DerivedTransaction): ZIO[Any, RepositoryError, Int]
  def modify(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int]

}

object TransactionRepository {

  type TYPE_ = DerivedTransaction
  def create(model: FinancialsTransaction): ZIO[TransactionRepository, RepositoryError, Int]                      =
    ZIO.service[TransactionRepository] flatMap (_.create(model))
  def create(item: TYPE_): ZIO[TransactionRepository, RepositoryError, Int]                                       =
    ZIO.service[TransactionRepository] flatMap (_.create(item))
  def create(items: List[TYPE_]): ZIO[TransactionRepository, RepositoryError, Int]                                =
    ZIO.service[TransactionRepository] flatMap (_.create(items))
  def create2(models: List[FinancialsTransaction]): ZIO[TransactionRepository, RepositoryError, Int]              =
    ZIO.collectAll(models.map(create(_))).map(_.sum)
  // ZIO.service[TransactionRepository] flatMap (_.create2(models))
  def delete(id: Long, company: String): ZIO[TransactionRepository, RepositoryError, Int]                         =
    ZIO.service[TransactionRepository] flatMap (_.delete(id, company))
  def delete(ids: List[Long], company: String): ZIO[TransactionRepository, RepositoryError, List[Int]]            =
    ZIO.collectAll(ids.map(delete(_, company)))
  def all(companyId: String): ZStream[TransactionRepository, RepositoryError, FinancialsTransaction]              =
    ZStream.service[TransactionRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[TransactionRepository, RepositoryError, TYPE_]                               =
    ZStream.service[TransactionRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[TransactionRepository, RepositoryError, TYPE_]                      =
    ZIO.service[TransactionRepository] flatMap (_.getBy(id, company))
  def getByTransId(id: Long, company: String): ZIO[TransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.service[TransactionRepository] flatMap (_.getByTransId(id, company))
  def getByModelId(modelid: Int, company: String): ZStream[TransactionRepository, RepositoryError, TYPE_]         =
    ZStream.service[TransactionRepository] flatMap (_.getByModelId(modelid, company))

  def getByModelIdX(modelId: Int, companyId: String): ZStream[TransactionRepository, RepositoryError, FinancialsTransaction]               =
    ZStream.service[TransactionRepository] flatMap (_.getByModelIdX(modelId, companyId))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[TransactionRepository, RepositoryError, FinancialsTransaction] =
    ZStream.service[TransactionRepository] flatMap (_.find4Period(fromPeriod, toPeriod, company))
  def modify(model: FinancialsTransaction): ZIO[TransactionRepository, RepositoryError, Int]                                               =
    ZIO.service[TransactionRepository] flatMap (_.modify(model))
  def modify(model: DerivedTransaction): ZIO[TransactionRepository, RepositoryError, Int]                                                  =
    ZIO.service[TransactionRepository] flatMap (_.modify(model))
  def modify(models: List[DerivedTransaction]): ZIO[TransactionRepository, RepositoryError, Int]                                           =
    ZIO.service[TransactionRepository] flatMap (_.modify(models))

}
