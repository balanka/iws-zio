package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{Journal, PeriodicAccountBalance}
import zio._

trait TransactionService {

  def post(id: Long, company: String): ZIO[Any, RepositoryError, Int]
  def postAll(ids: List[Long], company: String): ZIO[Any, RepositoryError, Int]

  def getBy(id: String, company: String): ZIO[Any, RepositoryError, PeriodicAccountBalance]
  def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]]
  def getByIds(ids: List[String], company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]
  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int]

}
object TransactionService {
  def post(id: Long, company: String): ZIO[TransactionService, RepositoryError, Int]=
    ZIO.service[TransactionService] flatMap (_.post(id, company))
  def postAll(ids: List[Long], company: String): ZIO[TransactionService, RepositoryError, Int]=
    ZIO.service[TransactionService] flatMap (_.postAll(ids, company))
  def getBy(id: String, company: String): ZIO[TransactionService, RepositoryError, PeriodicAccountBalance]=
    ZIO.serviceWithZIO[TransactionService](_.getBy(id, company))
  def journal(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[TransactionService, RepositoryError, List[Journal]]=
    ZIO.service[TransactionService] flatMap (_.journal(accountId, fromPeriod, toPeriod, company))
  def getByIds(ids: List[String], company: String): ZIO[TransactionService, RepositoryError, List[PeriodicAccountBalance]]=
    ZIO.serviceWithZIO[TransactionService](_.getByIds(ids, company))
  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[TransactionService, RepositoryError, Int]=
    ZIO.serviceWithZIO[TransactionService](_.postTransaction4Period(fromPeriod, toPeriod, company))
}

