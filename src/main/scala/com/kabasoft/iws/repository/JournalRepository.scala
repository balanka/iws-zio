package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Journal
import zio._

trait JournalRepository:
  def create(item: Journal): ZIO[Any, RepositoryError, Int]
  def create(models: List[Journal]): ZIO[Any, RepositoryError, Int]
  def all(Id: (Int, String)): ZIO[Any, RepositoryError, List[Journal]]
  def getById(Id: (Long, String)): ZIO[Any, RepositoryError, Journal]
  def getByPeriod(period:Int,  company: String): ZIO[Any, RepositoryError, List[Journal]]
  def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZIO[Any, RepositoryError, List[Journal]]
  def deleteAllTest(): ZIO[Any, RepositoryError, Int]

object JournalRepository:

  def create(item: Journal): ZIO[JournalRepository, RepositoryError, Int]               =
    ZIO.serviceWithZIO[JournalRepository](_.create(item))
  def create(items: List[Journal]): ZIO[JournalRepository, RepositoryError, Int]         =
    ZIO.serviceWithZIO[JournalRepository](_.create(items))
  
  def all(Id: (Int, String)): ZIO[JournalRepository, RepositoryError, List[Journal]]        =
    ZIO.serviceWithZIO[JournalRepository](_.all(Id))
    
  def getById(Id: (Long,  String)): ZIO[JournalRepository, RepositoryError, Journal] =
    ZIO.serviceWithZIO[JournalRepository](_.getById(Id))

  def getByPeriod(period: Int,  company: String): ZIO[JournalRepository, RepositoryError, List[Journal]] =
    ZIO.serviceWithZIO[JournalRepository](_.getByPeriod(period, company))

  def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int,  company: String): ZIO[JournalRepository, RepositoryError, List[Journal]] =
    ZIO.serviceWithZIO[JournalRepository] (_.find4Period(accountId, fromPeriod, toPeriod, company))
  
  def deleteAllTest(): ZIO[JournalRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[JournalRepository] (_.deleteAllTest())
