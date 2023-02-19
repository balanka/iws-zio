package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PeriodicAccountBalance
import zio._
import zio.stream._

trait PacRepository {

  // def create(item: PeriodicAccountBalance): IO[RepositoryError, Unit]
  def create(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int]
  def delete(item: String, company: String): IO[RepositoryError, Int]
  def delete(items: List[String], company: String): IO[RepositoryError, List[Int]]                    =
    ZIO.collectAll(items.map(delete(_, company)))
  def all(companyId: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]
  def list(company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def getBy(id: String, company: String): IO[RepositoryError, PeriodicAccountBalance]
  def getByIds(ids: List[String], company: String): IO[RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.foreach(ids)(getBy(_, company)).map(_.filterNot(x => x.id == PeriodicAccountBalance.dummy.id))

  def getByModelId(modelid: Int, company: String): IO[RepositoryError, PeriodicAccountBalance]
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def getBalances4Period(fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  // def modify(model: PeriodicAccountBalance): ZIO[Any, RepositoryError, Int]
  def modify(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int]

}

object PacRepository {

  // def create(item: PeriodicAccountBalance): ZIO[PacRepository, RepositoryError, Unit]                                                      =
  //  ZIO.service[PacRepository] flatMap (_.create(item))
  def create(items: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int]                                                =
    ZIO.service[PacRepository] flatMap (_.create(items))
  def delete(item: String, company: String): ZIO[PacRepository, RepositoryError, Int]                                                      =
    ZIO.service[PacRepository] flatMap (_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[PacRepository, RepositoryError, List[Int]]                                         =
    ZIO.foreach(items)(delete(_, company))
  def all(companyId: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]]                                            =
    ZIO.service[PacRepository] flatMap (_.all(companyId))
  def list(company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]                                               =
    ZStream.service[PacRepository] flatMap (_.list(company))
  def getBy(id: String, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance]                                      =
    ZIO.service[PacRepository] flatMap (_.getBy(id, company))
  def getByIds(ids: List[String], company: String): ZIO[PacRepository, RepositoryError, List[PeriodicAccountBalance]]                      =
    ZIO.foreach(ids)(getBy(_, company)).map(_.filterNot(x => x.id == PeriodicAccountBalance.dummy.id))
  def getByModelId(modelid: Int, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance]                             =
    ZIO.service[PacRepository] flatMap (_.getByModelId(modelid, company))
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance] =
    ZStream.service[PacRepository] flatMap (_.findBalance4Period(fromPeriod, toPeriod, company))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]        =
    ZStream.service[PacRepository] flatMap (_.find4Period(fromPeriod, toPeriod, company))
  def find4Period(
    accountId: String,
    fromPeriod: Int,
    toPeriod: Int,
    company: String
  ): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance] =
    ZStream.service[PacRepository] flatMap (_.find4Period(accountId, fromPeriod, toPeriod, company))
  def getBalances4Period(fromPeriod: Int, toPeriod: Int, company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance] =
    ZStream.service[PacRepository] flatMap (_.getBalances4Period(fromPeriod, toPeriod, company))
  // def modify(model: PeriodicAccountBalance): ZIO[PacRepository, RepositoryError, Int]                                                      =
  //  ZIO.service[PacRepository] flatMap (_.modify(model))
  def modify(models: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int]                                               =
    ZIO.service[PacRepository] flatMap (_.modify(models))

}
