package com.kabasoft.iws.service

import com.kabasoft.iws.domain.{ DerivedTransaction, FinancialsTransaction, PeriodicAccountBalance }
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait FinancialsService {
  // def closePeriod(fromPeriod: Int, toPeriod: Int,  inStmtAccId:String, company: String):ZIO[Any, RepositoryError, Int]
  def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int]
  def create(item: DerivedTransaction): ZIO[Any, RepositoryError, Int]
  def create(models: List[DerivedTransaction]): ZIO[Any, RepositoryError, Int]
  def post(model: FinancialsTransaction, company: String): ZIO[Any, RepositoryError, Int]

  def post(model: DerivedTransaction, company: String): ZIO[Any, RepositoryError, List[Int]]

  def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]]

  def getBy(id: String, company: String): ZIO[FinancialsService, RepositoryError, PeriodicAccountBalance]
  def getByIds(
    ids: List[String],
    company: String
  ): ZIO[FinancialsService, RepositoryError, List[PeriodicAccountBalance]]

  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Int]]

}

object FinancialsService {

  def create(model: FinancialsTransaction): ZIO[FinancialsService, RepositoryError, Int]                   =
    ZIO.serviceWithZIO[FinancialsService](_.create(model))
  def create(item: DerivedTransaction): ZIO[FinancialsService, RepositoryError, Int]                       =
    ZIO.serviceWithZIO[FinancialsService](_.create(item))
  def create(items: List[DerivedTransaction]): ZIO[FinancialsService, RepositoryError, Int]                =
    ZIO.serviceWithZIO[FinancialsService](_.create(items))
  def post(model: FinancialsTransaction, company: String): ZIO[FinancialsService, RepositoryError, Int]=
    ZIO.serviceWithZIO[FinancialsService](_.post(model, company))

  def post(model: DerivedTransaction, company: String): ZIO[FinancialsService, RepositoryError, List[Int]] =
    ZIO.serviceWithZIO[FinancialsService](_.post(model, company))

  def postAll(ids: List[Long], company: String): ZIO[FinancialsService, RepositoryError, List[Int]] =
    ZIO.serviceWithZIO[FinancialsService](_.postAll(ids, company))

  def getBy(id: String, company: String): ZIO[FinancialsService, RepositoryError, PeriodicAccountBalance] =
    ZIO.serviceWithZIO[FinancialsService](_.getBy(id, company))
  def getByIds(
    ids: List[String],
    company: String
  ): ZIO[FinancialsService, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[FinancialsService](_.getByIds(ids, company))

  def postTransaction4Period(
    fromPeriod: Int,
    toPeriod: Int,
    company: String
  ): ZIO[FinancialsService, RepositoryError, List[Int]] =
    ZIO.serviceWithZIO[FinancialsService](_.postTransaction4Period(fromPeriod, toPeriod, company))
}
