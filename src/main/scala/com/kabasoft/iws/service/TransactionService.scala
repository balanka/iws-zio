package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Journal, PeriodicAccountBalance}
import zio._

trait TransactionService {

  def post(id: Long, company: String): ZIO[Any, RepositoryError, Int]

  def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int]

  def getBy(id: String, company: String): ZIO[TransactionService, RepositoryError, PeriodicAccountBalance]
  def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]]
  def getByIds(ids: List[String], company: String): ZIO[TransactionService, RepositoryError, List[PeriodicAccountBalance]]

  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int]

}


