package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Journal, PeriodicAccountBalance }
import zio._

trait FinancialsService {

  def post(id: Long, company: String): ZIO[Any, RepositoryError, Int]
  // def post(model: DerivedTransaction, company: String): ZIO[Any, RepositoryError, List[Int]]

  def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int]

  def getBy(id: String, company: String): ZIO[FinancialsService, RepositoryError, PeriodicAccountBalance]
  def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]]
  def getByIds(ids: List[String], company: String): ZIO[FinancialsService, RepositoryError, List[PeriodicAccountBalance]]

  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int]

}

object FinancialsService {

//  def create(model: FinancialsTransaction): ZIO[FinancialsService, RepositoryError, Int]    =
//    ZIO.service[FinancialsService].flatMap(_.create(model))
//
//  def create(items: List[FinancialsTransaction]): ZIO[FinancialsService, RepositoryError, Int] =
//    ZIO.service[FinancialsService] flatMap (_.create(items))
  // def post(model: FinancialsTransaction, company: String): ZIO[FinancialsService, RepositoryError, Int]=
  //  ZIO.service[FinancialsService]flatMap(_.post(model, company))
  def post(id: Long, company: String): ZIO[FinancialsService, RepositoryError, Int]         =
    ZIO.service[FinancialsService] flatMap (_.post(id, company))
  // def post(model: DerivedTransaction, company: String): ZIO[FinancialsService, RepositoryError, List[Int]] =
  //   ZIO.service[FinancialsService]flatMap(_.post(model, company))

  def postAll(ids: List[Long], company: String): ZIO[FinancialsService, RepositoryError, Int]                                       =
    ZIO.service[FinancialsService] flatMap (_.postAll(ids, company))
  def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[FinancialsService, RepositoryError, List[Journal]] =
    ZIO.service[FinancialsService] flatMap (_.journal(accountId, fromPeriod, toPeriod, company))

  def getBy(id: String, company: String): ZIO[FinancialsService, RepositoryError, PeriodicAccountBalance]                 =
    ZIO.serviceWithZIO[FinancialsService](_.getBy(id, company))
  def getByIds(ids: List[String], company: String): ZIO[FinancialsService, RepositoryError, List[PeriodicAccountBalance]] =
    ZIO.serviceWithZIO[FinancialsService](_.getByIds(ids, company))

  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[FinancialsService, RepositoryError, Int] =
    ZIO.serviceWithZIO[FinancialsService](_.postTransaction4Period(fromPeriod, toPeriod, company))
}
