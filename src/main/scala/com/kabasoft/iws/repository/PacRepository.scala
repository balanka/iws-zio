package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PeriodicAccountBalance
import zio._
import zio.stream._

trait PacRepository {

  def create(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int]
  def all(companyId: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]
  def list(company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def getBy(id: String, company: String): IO[RepositoryError, PeriodicAccountBalance]
  def getByIds(ids: List[String], company: String): IO[RepositoryError, List[PeriodicAccountBalance]]
  def getByModelId(modelid: Int, company: String): IO[RepositoryError, PeriodicAccountBalance]
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def find4Period(accountId: String,  toPeriod: Int, company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def getBalances4Period( toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def modify(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int]


}

object PacRepository {
  def create(items: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int]                                                =
    ZIO.serviceWithZIO[PacRepository](_.create(items))
  def all(companyId: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]]                                            =
    ZIO.serviceWithZIO[PacRepository](_.all(companyId))
  def list(company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]                                               =
    ZStream.service[PacRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance]                                      =
    ZIO.serviceWithZIO[PacRepository](_.getBy(id, company))
  def getByIds(ids: List[String], companyId: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]]                    =
    ZIO.serviceWithZIO[PacRepository](_.getByIds(ids, companyId))
  def getByModelId(modelid: Int, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance]                             =
    ZIO.serviceWithZIO[PacRepository](_.getByModelId(modelid, company))
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance] =
    ZStream.service[PacRepository] flatMap (_.findBalance4Period(fromPeriod, toPeriod, company))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]        =
    ZStream.service[PacRepository] flatMap (_.find4Period(fromPeriod, toPeriod, company))
  def find4Period(accountId: String,  toPeriod: Int, company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance] =
    ZStream.service[PacRepository] flatMap (_.find4Period(accountId,  toPeriod, company))
  def getBalances4Period(toPeriod: Int, company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance] =
    ZStream.service[PacRepository] flatMap (_.getBalances4Period(toPeriod, company))
  def modify(models: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int]                                               =
    ZIO.serviceWithZIO[PacRepository](_.modify(models))

}
