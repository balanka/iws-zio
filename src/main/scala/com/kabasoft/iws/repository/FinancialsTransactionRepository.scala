package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import zio._
import zio.stream._

trait FinancialsTransactionRepository {
  def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction]
  def create(item: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def delete(item: Long, company: String): ZIO[Any, RepositoryError, Int]
  def delete(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]] =
    ZIO.foreach(ids)(delete(_, company))
  def all(companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getByTransId(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getByTransId1(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]

  def getByIds(ids: List[Long], companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getByModelId(modelid:(Int,  String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, FinancialsTransaction]
  def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]
  def update(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction]
   def updatePostedField(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def updatePostedField(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]

}

object FinancialsTransactionRepository {

  def create(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.create(model))
  def create(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]]              =
   ZIO.serviceWithZIO[FinancialsTransactionRepository](_.create(models))
  def delete(id: Long, company: String): ZIO[FinancialsTransactionRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.delete(id, company))
  def delete(ids: List[Long], company: String): ZIO[FinancialsTransactionRepository, RepositoryError, List[Int]]            =
    ZIO.foreach(ids)(delete(_, company))
  def all(companyId: String): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]]             =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.all(companyId))
  def getByTransId(id:(Long,  String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByTransId(id))

  def getByIds(ids: List[Long], companyId: String): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByIds(ids, companyId))
  def getByTransId1(id: (Long, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByTransId1(id))
  def getByModelId(modelid:(Int,  String)): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]]         =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByModelId(modelid))
  def getByModelIdX(modelId: Int, companyId: String): ZStream[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction]               =
    ZStream.service[FinancialsTransactionRepository] flatMap (_.getByModelIdX(modelId, companyId))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZStream.service[FinancialsTransactionRepository] flatMap (_.find4Period(fromPeriod, toPeriod, company))
  def modify(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, Int]                                               =
    ZIO.service[FinancialsTransactionRepository] flatMap (_.modify(model))
  def modify(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.service[FinancialsTransactionRepository] flatMap (_.modify(models))

  def update(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.service[FinancialsTransactionRepository] flatMap (_.update(model))
  def updatePostedField(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.service[FinancialsTransactionRepository] flatMap (_.updatePostedField(model))
   def updatePostedField(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int]=
     ZIO.service[FinancialsTransactionRepository] flatMap (_.updatePostedField(models))
}
