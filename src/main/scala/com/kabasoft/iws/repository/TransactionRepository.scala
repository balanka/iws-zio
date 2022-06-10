package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait TransactionRepository {
  type TYPE_ = DerivedTransaction
  def create(item: TYPE_): ZIO[Any, RepositoryError, Int]
  def create(models: List[TYPE_]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): ZIO[Any, RepositoryError, Int]
  def delete(items: List[String], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): ZIO[Any, RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): ZStream[Any, RepositoryError, TYPE_]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, DerivedTransaction]
  def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def modify(model: DerivedTransaction): ZIO[Any, RepositoryError, Int]
  def modify(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int] // =
  // ZIO.collectAll(models.map(modify(_)))

}

object TransactionRepository {

  type TYPE_ = DerivedTransaction
  def create(item: TYPE_): ZIO[TransactionRepository, RepositoryError, Int]                                =
    ZIO.serviceWithZIO[TransactionRepository](_.create(item))
  def create(items: List[TYPE_]): ZIO[TransactionRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[TransactionRepository](_.create(items))
  def delete(item: String, company: String): ZIO[TransactionRepository, RepositoryError, Int]              =
    ZIO.serviceWithZIO[TransactionRepository](_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[TransactionRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[TransactionRepository, RepositoryError, TYPE_]                        =
    ZStream.serviceWithStream[TransactionRepository](_.list(company))
  def getBy(id: String, company: String): ZIO[TransactionRepository, RepositoryError, TYPE_]               =
    ZIO.serviceWithZIO[TransactionRepository](_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZStream[TransactionRepository, RepositoryError, TYPE_]  =
    ZStream.serviceWithStream[TransactionRepository](_.getByModelId(modelid, company))
  def find4Period(
    fromPeriod: Int,
    toPeriod: Int,
    company: String
  ): ZStream[TransactionRepository, RepositoryError, TYPE_] =
    ZStream.serviceWithStream[TransactionRepository](_.find4Period(fromPeriod, toPeriod, company))
  def modify(model: FinancialsTransaction): ZIO[TransactionRepository, RepositoryError, Int]               =
    ZIO.serviceWithZIO[TransactionRepository](_.modify(model))
  def modify(model: DerivedTransaction): ZIO[TransactionRepository, RepositoryError, Int]                  =
    ZIO.serviceWithZIO[TransactionRepository](_.modify(model))
  def modify(models: List[DerivedTransaction]): ZIO[TransactionRepository, RepositoryError, Int]           =
    ZIO.serviceWithZIO[TransactionRepository](_.modify(models))

  // def modify(models: List[DerivedTransaction]): ZIO[TransactionRepository, RepositoryError, Int]=
  // ZIO.serviceWithZIO[TransactionRepository](_.modify(models))
//   ZIO.collectAll(models.map(modify(_))).map(_.sum)

}
