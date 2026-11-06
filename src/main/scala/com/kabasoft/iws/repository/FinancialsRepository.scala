package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import zio._

trait FinancialsRepository:

  def create(item: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction]
  def create(models: List[FinancialsTransaction]):ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction]
  def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def alln(Id: (List[Int], String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def getById(Id: (Long, Int, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  //def getById1(Id: (Long, Int, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int]
  def deleteAll(p: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]
  def getByTransId(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]
  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, company: String, posted:Boolean): ZIO[Any, RepositoryError, List[FinancialsTransaction]]




object FinancialsRepository:

  def create(model: FinancialsTransaction): ZIO[FinancialsRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsRepository](_.create(model))

  def create(models: List[FinancialsTransaction]): ZIO[FinancialsRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsRepository](_.create(models))
  
  def delete(id: Long, modelid: Int, company: String): ZIO[FinancialsRepository, RepositoryError, Int] =
      ZIO.serviceWithZIO[FinancialsRepository](_.delete(id, modelid, company))
  
  def deleteAll(p: List[FinancialsTransaction]): ZIO[FinancialsRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsRepository](_.deleteAll(p))
  def all(Id: (Int, String)): ZIO[FinancialsRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsRepository](_.all(Id))

  def alln(Id: (List[Int], String)): ZIO[FinancialsRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsRepository](_.alln(Id))
      
  def getByTransId(id: (Long, String)): ZIO[FinancialsRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsRepository](_.getByTransId(id))
  def getBy(ids: List[Long], modelid: Int, companyId: String): ZIO[FinancialsRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsRepository](_.getBy(ids, modelid, companyId))
  def getById(p:(Long, Int, String)): ZIO[FinancialsRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsRepository](_.getById(p))

//  def getById1(p: (Long, Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
//    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getById1(p))   

  def getByModelId(modelid: (Int, String)): ZIO[FinancialsRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsRepository](_.getByModelId(modelid))
  
  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, company: String, posted:Boolean): ZIO[FinancialsRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.service[FinancialsRepository] flatMap (_.find4Period(fromPeriod, toPeriod, modelid, company, posted))

  def modify(model: FinancialsTransaction): ZIO[FinancialsRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsRepository](_.modify(model))

  def modify(models: List[FinancialsTransaction]): ZIO[FinancialsRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsRepository](_.modify(models))


