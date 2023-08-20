package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import zio._
import zio.stream._

trait TransactionRepository {
  def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction]
  def create(item: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def delete(item: Long, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.foreach(ids)(delete(_, company))
  def all(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getByTransId(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getByTransId1(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getByModelId(modelid:(Int,  String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, FinancialsTransaction]
  def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]
  def update(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction]
   def updatePostedField(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def updatePostedField(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]

}

object TransactionRepository {

  def create(model: FinancialsTransaction): ZIO[TransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.service[TransactionRepository] flatMap (_.create(model))
  def create(models: List[FinancialsTransaction]): ZIO[TransactionRepository, RepositoryError, List[FinancialsTransaction]]              =
   ZIO.service[TransactionRepository] flatMap (_.create(models))
  def delete(id: Long, company: String): ZIO[TransactionRepository, RepositoryError, Int]                         =
    ZIO.service[TransactionRepository] flatMap (_.delete(id, company))
  def delete(ids: List[Long], company: String): ZIO[TransactionRepository, RepositoryError, List[Int]]            =
    ZIO.foreach(ids)(delete(_, company))
  def all(companyId: String): ZIO[TransactionRepository, RepositoryError, List[FinancialsTransaction]]             =
    ZIO.service[TransactionRepository] flatMap (_.all(companyId))
  def getByTransId(id:(Long,  String)): ZIO[TransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.service[TransactionRepository] flatMap (_.getByTransId(id))

  def getByTransId1(id: (Long, String)): ZIO[TransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.service[TransactionRepository] flatMap (_.getByTransId1(id))
  def getByModelId(modelid:(Int,  String)): ZIO[TransactionRepository, RepositoryError, List[FinancialsTransaction]]         =
    ZIO.service[TransactionRepository] flatMap (_.getByModelId(modelid))
  def getByModelIdX(modelId: Int, companyId: String): ZStream[TransactionRepository, RepositoryError, FinancialsTransaction]               =
    ZStream.service[TransactionRepository] flatMap (_.getByModelIdX(modelId, companyId))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[TransactionRepository, RepositoryError, FinancialsTransaction] =
    ZStream.service[TransactionRepository] flatMap (_.find4Period(fromPeriod, toPeriod, company))
  def modify(model: FinancialsTransaction): ZIO[TransactionRepository, RepositoryError, Int]                                               =
    ZIO.service[TransactionRepository] flatMap (_.modify(model))
  def modify(models: List[FinancialsTransaction]): ZIO[TransactionRepository, RepositoryError, Int] =
    ZIO.service[TransactionRepository] flatMap (_.modify(models))

  def update(model: FinancialsTransaction): ZIO[TransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.service[TransactionRepository] flatMap (_.update(model))
  def updatePostedField(model: FinancialsTransaction): ZIO[TransactionRepository, RepositoryError, Int] =
    ZIO.service[TransactionRepository] flatMap (_.updatePostedField(model))
   def updatePostedField(models: List[FinancialsTransaction]): ZIO[TransactionRepository, RepositoryError, Int]=
     ZIO.service[TransactionRepository] flatMap (_.updatePostedField(models))
}
