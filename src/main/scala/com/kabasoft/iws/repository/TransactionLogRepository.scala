package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.TransactionLog
import zio._
import zio.stream._

trait TransactionLogRepository {

  def create(item: TransactionLog): IO[RepositoryError, Unit]
  def create(models: List[TransactionLog]): IO[RepositoryError, Int]
  def delete(Id: Long, company: String): IO[RepositoryError, Int]
  def list(company: String): ZStream[Any, RepositoryError, TransactionLog]
  def getBy(id: Long, company: String): IO[RepositoryError, TransactionLog]
  def getByModelId(modelid: Int, company: String): IO[RepositoryError, TransactionLog]
  def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZStream[Any, RepositoryError, TransactionLog]

}

object TransactionLogRepository {

  def create(item: TransactionLog): ZIO[TransactionLogRepository, RepositoryError, Unit]               =
    ZIO.service[TransactionLogRepository] flatMap (_.create(item))
  def create(items: List[TransactionLog]): ZIO[TransactionLogRepository, RepositoryError, Int]         =
    ZIO.service[TransactionLogRepository] flatMap (_.create(items))
  def delete(Id: Long, company: String): ZIO[TransactionLogRepository, RepositoryError, Int]    =
    ZIO.service[TransactionLogRepository] flatMap (_.delete(Id, company))
  def list(company: String): ZStream[TransactionLogRepository, RepositoryError, TransactionLog]        =
    ZStream.service[TransactionLogRepository] flatMap (_.list(company))
  def getBy(id: Long, company: String): ZIO[TransactionLogRepository, RepositoryError, TransactionLog] =
    ZIO.service[TransactionLogRepository] flatMap (_.getBy(id, company))

  def getByModelId(modelid: Int, company: String): ZIO[TransactionLogRepository, RepositoryError, TransactionLog] =
    ZIO.service[TransactionLogRepository] flatMap (_.getByModelId(modelid, company))

  def find4Period(accountId: String, fromPeriod: Int, toPeriod: Int, company: String): ZStream[TransactionLogRepository, RepositoryError, TransactionLog] =
    ZStream.service[TransactionLogRepository] flatMap (_.find4Period(accountId, fromPeriod, toPeriod, company))

}
