package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait PacRepository {
 // type TYPE_ = PeriodicAccountBalance
  def create(item: PeriodicAccountBalance): IO[RepositoryError, Unit]
  def create(models: List[PeriodicAccountBalance]): IO[ RepositoryError, Int]
  def delete(item: String, company: String): IO[RepositoryError, Int]
  def delete(items: List[String], company: String): IO[RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def getBy(id: String, company: String): IO[RepositoryError, PeriodicAccountBalance]
 // def getBy(ids:List[String], company: String): ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def getByModelId(modelid: Int, company: String): IO[RepositoryError, PeriodicAccountBalance]
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def getBalances4Period(fromPeriod: Int, toPeriod: Int, companyId: String):ZStream[Any, RepositoryError, PeriodicAccountBalance]
  def modify(model: PeriodicAccountBalance): ZIO[Any, RepositoryError, Int]
  def modify(models: List[PeriodicAccountBalance]): ZIO[Any, RepositoryError, Int]
  // def update(model:TYPE_, company: String): ZIO[Any, RepositoryError, Int]
  // def findSome(company: String, param: Seq[String]): ZStream[Any, RepositoryError, TYPE_]
}

object PacRepository {

  def create(item: PeriodicAccountBalance): ZIO[PacRepository, RepositoryError, Unit]                               =
    ZIO.serviceWithZIO[PacRepository](_.create(item))
  def create(items: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[PacRepository](_.create(items))
  def delete(item: String, company: String): ZIO[PacRepository, RepositoryError, Int]              =
    ZIO.serviceWithZIO[PacRepository](_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[PacRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]                        =
    ZStream.serviceWithStream[PacRepository](_.list(company))
  def getBy(id: String, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance]               =
    ZIO.serviceWithZIO[PacRepository](_.getBy(id, company))
  //def getBy(ids:List[String], company: String): ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]=
  //  ZStream.serviceWithStream[PacRepository](_.getBy(ids,company))
  def getByModelId(modelid: Int, company: String): ZIO[PacRepository, RepositoryError, PeriodicAccountBalance]      =
    ZIO.serviceWithZIO[PacRepository](_.getByModelId(modelid, company))
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]=
    ZStream.serviceWithStream[PacRepository](_.findBalance4Period(fromPeriod, toPeriod, company))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]=
    ZStream.serviceWithStream[PacRepository](_.find4Period(fromPeriod, toPeriod, company))
  def getBalances4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[PacRepository, RepositoryError, PeriodicAccountBalance]=
    ZStream.serviceWithStream[PacRepository](_.getBalances4Period(fromPeriod, toPeriod, company))
  def modify(model: PeriodicAccountBalance): ZIO[PacRepository, RepositoryError, Int]=
    ZIO.serviceWithZIO[PacRepository](_.modify(model))
  def modify(models: List[PeriodicAccountBalance]): ZIO[PacRepository, RepositoryError, Int] =
    ZIO.serviceWithZIO[PacRepository](_.modify(models))

}
