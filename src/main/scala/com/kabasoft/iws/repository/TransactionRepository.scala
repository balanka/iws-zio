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
  // def modify(model: TYPE_): ZIO[Any, RepositoryError, Int]
  // def findTransactionWithDetails(): ZStream[Any, RepositoryError, DerivedTransaction]
  // def update(models: List[TYPE_], company: String): ZIO[Any, RepositoryError, List[Int]]
  // def update(model:TYPE_, company: String): ZIO[Any, RepositoryError, Int]
  // def findSome(company: String, param: Seq[String]): ZStream[Any, RepositoryError, TYPE_]
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
  // def modify(model: TYPE_): ZIO[TransactionRepository, RepositoryError, Int]=
  // ZIO.serviceWithZIO[TransactionRepository](_.modify(model))
  // def update(models: List[TYPE_], company: String): ZIO[TransactionRepository, RepositoryError, Int]=
  //  ZIO.collectAll(models.map(update(_, company)))
  // def update(model: TYPE_, company: String): ZIO[TransactionRepository, RepositoryError, Int]=
  //   ZIO.serviceWithZIO[TransactionRepository](_.update(model, company))
  // def findSome(company: String, param: String*): ZStream[TransactionRepository, RepositoryError, TYPE_]=
  //  ZStream.serviceWithStream[TransactionRepository](_.findSome(company,param))

}
