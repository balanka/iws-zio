package com.kabasoft.iws.service

import com.kabasoft.iws.domain.DerivedTransaction
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait FinancialsService {
  // def closePeriod(fromPeriod: Int, toPeriod: Int,  inStmtAccId:String, company: String):ZIO[Any, RepositoryError, Int]

  def post(model: DerivedTransaction, company: String): ZIO[Any, RepositoryError, List[Int]]

  def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, List[Int]]

  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Int]]

}

object FinancialsService {

  def post(model: DerivedTransaction, company: String): ZIO[FinancialsService, RepositoryError, List[Int]] =
    ZIO.serviceWithZIO[FinancialsService](_.post(model, company))

  def postAll(ids: List[Long], company: String): ZIO[FinancialsService, RepositoryError, List[Int]] =
    ZIO.serviceWithZIO[FinancialsService](_.postAll(ids, company))

  def postTransaction4Period(
    fromPeriod: Int,
    toPeriod: Int,
    company: String
  ): ZIO[FinancialsService, RepositoryError, List[Int]] =
    ZIO.serviceWithZIO[FinancialsService](_.postTransaction4Period(fromPeriod, toPeriod, company))
}
