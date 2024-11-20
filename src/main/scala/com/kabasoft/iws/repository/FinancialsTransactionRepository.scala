package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails}
import zio.*
import zio.stream.*

trait FinancialsTransactionRepository:

  def create(item: FinancialsTransaction): ZIO[Any, RepositoryError, Int]

  def create(models: List[FinancialsTransaction]):ZIO[Any, RepositoryError, Int]

//  def update(model: FinancialsTransaction): Task[FinancialsTransaction]
//
//  def update(models: List[FinancialsTransaction]): Task[List[FinancialsTransaction]]

  def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]

  def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]

//  def details(p: (List[Long], String)): ZIO[Any, RepositoryError, List[FinancialsTransactionDetails]]
  
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]

  def getById(Id: (Long, Int, String)): ZIO[Any, RepositoryError, FinancialsTransaction]

  def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]

  def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int]

  // def delete(Ids:(List[Long], Int,  String)): Task[List[(Long, Int, String)]]

  def getByTransId(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]

//  def getByTransId1(id: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction]
  
  def getByIds(ids: List[Long], modelid: Int, companyId: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]]

  def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]]

//  def getByModelIdX(modelId: Int, companyId: String): ZStream[Any, RepositoryError, FinancialsTransaction]

  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, company: String, posted:Boolean): ZIO[Any, RepositoryError, List[FinancialsTransaction]]

  //def update(model: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction]
//  def updatePostedField(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
//
//  def updatePostedField(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int]



object FinancialsTransactionRepository:

  def create(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.create(model))

  def create(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.create(models))

  def delete(id: Long, modelid: Int, company: String): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.delete(id, modelid, company))

  //  def delete(Ids:(List[Long], Int,  String)): ZIO[FinancialsTransactionRepository, RepositoryError, List[(Long, Int, String)]]            =
  //    ZIO.foreach(ids)(delete(_, modelid, company)).mapError(e=>RepositoryError(e.getMessage))
//  def details(p: (List[Long], String)): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransactionDetails]]=
//    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.details(p))
    
  def all(Id: (Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.all(Id))

  def getByTransId(id: (Long, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByTransId(id))

  def getBy(ids: List[Long], modelid: Int, companyId: String): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getBy(ids, modelid, companyId))
  
  def getById(p:(Long, Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getById(p))
    
  def getByIds(ids: List[Long], modelid: Int, companyId: String): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByIds(ids, modelid, companyId))

//  def getByTransId1(id: (Long, String)): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
//    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByTransId1(id))

  def getByModelId(modelid: (Int, String)): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.getByModelId(modelid))

//  def getByModelIdX(modelId: Int, companyId: String): ZStream[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
//    ZStream.service[FinancialsTransactionRepository] flatMap (_.getByModelIdX(modelId, companyId))

  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, company: String, posted:Boolean): ZIO[FinancialsTransactionRepository, RepositoryError, List[FinancialsTransaction]] =
    ZIO.service[FinancialsTransactionRepository] flatMap (_.find4Period(fromPeriod, toPeriod, modelid, company, posted))

  def modify(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.modify(model))

  def modify(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.modify(models))

//  def update(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, FinancialsTransaction] =
//    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.update(model).mapError(e => RepositoryError(e.getMessage)))

//  def updatePostedField(model: FinancialsTransaction): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
//    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.updatePostedField(model))
//
//  def updatePostedField(models: List[FinancialsTransaction]): ZIO[FinancialsTransactionRepository, RepositoryError, Int] =
//    ZIO.serviceWithZIO[FinancialsTransactionRepository](_.updatePostedField(models))

