package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PeriodicAccountBalance
import zio._

trait PacRepository:

  def create(item: PeriodicAccountBalance):ZIO[Any, RepositoryError, Int]

  def create(models: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int]

//  def modify(model: PeriodicAccountBalance):ZIO[Any, RepositoryError, Int]
//
//  def modify(models: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int]
  def update(models: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int]

  def all(Id: (Int, String)):ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]

  def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, PeriodicAccountBalance]

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]

  def findBalance4Period(period: Int, company: String):ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String):ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]
  //def find4Period(fromPeriod: Int, toPeriod: Int, company: String): Task[List[PeriodicAccountBalance]]
  def find4AccountPeriod(accountId: String,  toPeriod: Int, companyId: String):ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]
  
  def deleteAll(): ZIO[Any, RepositoryError, Int]

object PacRepository:
  def create(item: PeriodicAccountBalance): ZIO[PacRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PacRepository](_.create(item))

  def create(models: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PacRepository](_.create(models))

//  def modify(model: PeriodicAccountBalance): ZIO[PacRepository, RepositoryError, Int] =
//    ZIO.serviceWithZIO[PacRepository](_.modify(model))
//
//  def modify(models: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int] =
//    ZIO.serviceWithZIO[PacRepository](_.update(models))  

  def update(models: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PacRepository](_.update(models))

  def all(Id: (Int, String)): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[PacRepository](_.all(Id))

  def getById(p: (String, Int, String)): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance] =
    ZIO.serviceWithZIO[PacRepository](_.getById(p))

  def getBy(ids: List[String], modelid: Int, company: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[PacRepository](_.getBy(ids, modelid, company))

  def findBalance4Period(period: Int, company: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[PacRepository](_.findBalance4Period(period, company))
    
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[PacRepository](_.findBalance4Period(fromPeriod, toPeriod, company))
  
  def find4AccountPeriod(accountId: String,  toPeriod: Int, companyId: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[PacRepository](_.find4AccountPeriod(accountId, toPeriod, companyId))

  def deleteAll(): ZIO[PacRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PacRepository](_.deleteAll())

//  def find4PeriodZ(accountId: String,  toPeriod: Int, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance] =
//    ZStream.service[PacRepository] flatMap (_.find4PeriodZ(accountId,  toPeriod, company)).mapError(e => RepositoryError(e.getMessage))
//  def getBalances4Period(toPeriod: Int, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance] =
//    ZStream.service[PacRepository] flatMap (_.getBalances4Period(toPeriod, company)).mapError(e => RepositoryError(e.getMessage))



