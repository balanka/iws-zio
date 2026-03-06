package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import zio._

trait FinancialsTransactionRepository:

  def create(item: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def create(models: List[FinancialsTransaction]):ZIO[Any, RepositoryError, Int]
  def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getById(Id: (Long, Int, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getById1(Id: (Long, Int, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]
  def getByTransId(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, company: String, posted:Boolean): ZIO[Any, RepositoryError, List[FinancialsTransaction]]




object FinancialsTransactionRepository:

  def create(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.create(model))

  def create(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.create(models))
  
  def delete(id: Long, modelid: Int, company: String): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
      ZIO.serviceWithZIO[FinancialsTransactionRepository](_.delete(id, modelid, company))
  
  def deleteAll(p: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.deleteAll(p))
  def all(Id: (Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.all(Id))
  def getByTransId(id: (Long, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByTransId(id))
  def getBy(ids: List[Long], modelid: Int, companyId: String): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getBy(ids, modelid, companyId))
  def getById(p:(Long, Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getById(p))

  def getById1(p: (Long, Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getById1(p))   

  def getByModelId(modelid: (Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByModelId(modelid))
  
  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, company: String, posted:Boolean): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.service[FinancialsTransactionRepository] flatMap (_.find4Period(fromPeriod, toPeriod, modelid, company, posted))

  def modify(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.modify(model))

  def modify(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.modify(models))


