package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Transaction
import zio._
import zio.stream._

trait TransactionRepository {
  def create(model: Transaction): ZIO[Any, RepositoryError, Transaction]
  def create(item: List[Transaction]): ZIO[Any, RepositoryError, List[Transaction]]
  def create2(transactions: List[Transaction]): ZIO[Any, RepositoryError, Int]
  def delete(item: Long, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.foreach(ids)(delete(_, company))
  def all(companyId: String): ZIO[Any, RepositoryError, List[Transaction]]
  def getByTransId(id: (Long, String)): ZIO[Any, RepositoryError, Transaction]
  def getByTransId1(id: (Long, String)): ZIO[Any, RepositoryError, Transaction]

  def getByIds(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[Transaction]]
  def getByModelId(modelid:(Int,  String)): ZIO[Any, RepositoryError, List[Transaction]]
  def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Transaction]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, Transaction]
  def modify(model: Transaction): ZIO[Any, RepositoryError, Int]
  def modify(models: List[Transaction]): ZIO[Any, RepositoryError, Int]
  def update(model: Transaction): ZIO[Any, RepositoryError, Transaction]
   def updatePostedField(model: Transaction): ZIO[Any, RepositoryError, Int]
  def updatePostedField(models: List[Transaction]): ZIO[Any, RepositoryError, Int]

}

object TransactionRepository {

  def create(model: Transaction): ZIO[TransactionRepository, RepositoryError, Transaction] =
    ZIO.serviceWithZIO[TransactionRepository](_.create(model))
  def create(models: List[Transaction]): ZIO[TransactionRepository, RepositoryError, List[Transaction]]              =
   ZIO.serviceWithZIO[TransactionRepository](_.create(models))
  def create2(transactions: List[Transaction]): ZIO[TransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[TransactionRepository](_.create2(transactions))
  def delete(id: Long, company: String): ZIO[TransactionRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[TransactionRepository](_.delete(id, company))
  def delete(ids: List[Long], company: String): ZIO[TransactionRepository, RepositoryError, List[Int]]            =
    ZIO.foreach(ids)(delete(_, company))
  def all(companyId: String): ZIO[TransactionRepository, RepositoryError, List[Transaction]]             =
    ZIO.serviceWithZIO[TransactionRepository](_.all(companyId))
  def getByTransId(id:(Long,  String)): ZIO[TransactionRepository, RepositoryError, Transaction] =
    ZIO.serviceWithZIO[TransactionRepository](_.getByTransId(id))

  def getByIds(ids: List[Long], companyId: String): ZIO[TransactionRepository, RepositoryError, List[Transaction]] =
    ZIO.serviceWithZIO[TransactionRepository](_.getByIds(ids, companyId))
  def getByTransId1(id: (Long, String)): ZIO[TransactionRepository, RepositoryError, Transaction] =
    ZIO.serviceWithZIO[TransactionRepository](_.getByTransId1(id))
  def getByModelId(modelid:(Int,  String)): ZIO[TransactionRepository, RepositoryError, List[Transaction]]         =
    ZIO.serviceWithZIO[TransactionRepository](_.getByModelId(modelid))
  def getByModelIdX(modelId: Int, companyId: String): ZStream[TransactionRepository, RepositoryError, Transaction]               =
    ZStream.service[TransactionRepository] flatMap (_.getByModelIdX(modelId, companyId))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[TransactionRepository, RepositoryError, Transaction] =
    ZStream.service[TransactionRepository] flatMap (_.find4Period(fromPeriod, toPeriod, company))
  def modify(model: Transaction): ZIO[TransactionRepository, RepositoryError, Int]                                               =
    ZIO.service[TransactionRepository] flatMap (_.modify(model))
  def modify(models: List[Transaction]): ZIO[TransactionRepository, RepositoryError, Int] =
    ZIO.service[TransactionRepository] flatMap (_.modify(models))

  def update(model: Transaction): ZIO[TransactionRepository, RepositoryError, Transaction] =
    ZIO.service[TransactionRepository] flatMap (_.update(model))
  def updatePostedField(model: Transaction): ZIO[TransactionRepository, RepositoryError, Int] =
    ZIO.service[TransactionRepository] flatMap (_.updatePostedField(model))
   def updatePostedField(models: List[Transaction]): ZIO[TransactionRepository, RepositoryError, Int]=
     ZIO.service[TransactionRepository] flatMap (_.updatePostedField(models))
}
