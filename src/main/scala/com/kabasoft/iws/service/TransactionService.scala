package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait TransactionService:

  def post(id: (Long, Int), company: String): ZIO[Any, RepositoryError, Int]
  def postAll(ids: List[(Long, Int)], company: String): ZIO[Any, RepositoryError, Int]
  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, Int]


object TransactionService:
  def post(id: (Long, Int), company: String): ZIO[TransactionService, RepositoryError, Int]=
    ZIO.serviceWithZIO[TransactionService](_.post(id, company))
  def postAll(ids: List[(Long, Int)], company: String): ZIO[TransactionService, RepositoryError, Int]=
    ZIO.serviceWithZIO[TransactionService](_.postAll(ids, company))
  def postTransaction4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[TransactionService, RepositoryError, Int]=
    ZIO.serviceWithZIO[TransactionService](_.postTransaction4Period(fromPeriod, toPeriod, company))