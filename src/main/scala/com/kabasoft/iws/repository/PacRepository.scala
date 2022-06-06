package com.kabasoft.iws.repository

import zio.stream._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait PacRepository {
  type TYPE_ = PeriodicAccountBalance
  def create(item: TYPE_): IO[RepositoryError, Unit]
  def create(models: List[TYPE_]): IO[ RepositoryError, Int]
  def delete(item: String, company: String): IO[RepositoryError, Int]
  def delete(items: List[String], company: String): IO[RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[Any, RepositoryError, TYPE_]
  def getBy(id: String, company: String): IO[RepositoryError, TYPE_]
  def getByModelId(modelid: Int, company: String): IO[RepositoryError, TYPE_]
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[Any, RepositoryError, TYPE_]
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[Any, RepositoryError, TYPE_]
  // def update(models: List[TYPE_], company: String): ZIO[Any, RepositoryError, List[Int]]
  // def update(model:TYPE_, company: String): ZIO[Any, RepositoryError, Int]
  // def findSome(company: String, param: Seq[String]): ZStream[Any, RepositoryError, TYPE_]
}

object PacRepository {

  type TYPE_ = PeriodicAccountBalance
  def create(item: TYPE_): ZIO[PacRepository, RepositoryError, Unit]                               =
    ZIO.serviceWithZIO[PacRepository](_.create(item))
  def create(items: List[TYPE_]): ZIO[PacRepository, RepositoryError, Int]                         =
    ZIO.serviceWithZIO[PacRepository](_.create(items))
  def delete(item: String, company: String): ZIO[PacRepository, RepositoryError, Int]              =
    ZIO.serviceWithZIO[PacRepository](_.delete(item, company))
  def delete(items: List[String], company: String): ZIO[PacRepository, RepositoryError, List[Int]] =
    ZIO.collectAll(items.map(delete(_, company)))
  def list(company: String): ZStream[PacRepository, RepositoryError, TYPE_]                        =
    ZStream.serviceWithStream[PacRepository](_.list(company))
  def getBy(id: String, company: String): ZIO[PacRepository, RepositoryError, TYPE_]               =
    ZIO.serviceWithZIO[PacRepository](_.getBy(id, company))
  def getByModelId(modelid: Int, company: String): ZIO[PacRepository, RepositoryError, TYPE_]      =
    ZIO.serviceWithZIO[PacRepository](_.getByModelId(modelid, company))
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[PacRepository, RepositoryError, TYPE_]=
    ZStream.serviceWithStream[PacRepository](_.findBalance4Period(fromPeriod, toPeriod, company))
  def find4Period(fromPeriod: Int, toPeriod: Int, company: String):ZStream[PacRepository, RepositoryError, TYPE_]=
    ZStream.serviceWithStream[PacRepository](_.find4Period(fromPeriod, toPeriod, company))
  // def update(models: List[TYPE_], company: String): ZIO[PacRepository, RepositoryError, Int]=
  //  ZIO.collectAll(models.map(update(_, company)))
  // def update(model: TYPE_, company: String): ZIO[PacRepository, RepositoryError, Int]=
  //   ZIO.serviceWithZIO[PacRepository](_.update(model, company))
  // def findSome(company: String, param: String*): ZStream[PacRepository, RepositoryError, TYPE_]=
  //  ZStream.serviceWithStream[PacRepository](_.findSome(company,param))

}
