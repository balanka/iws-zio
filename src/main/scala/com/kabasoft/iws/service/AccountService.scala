package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait AccountService {
  def closePeriod(fromPeriod: Int, toPeriod: Int, inStmtAccId: String, company: String): ZIO[Any, RepositoryError, Int]
}

object AccountService {
  def closePeriod(
    fromPeriod: Int,
    toPeriod: Int,
    inStmtAccId: String,
    company: String
  ): ZIO[AccountService, RepositoryError, Int] =
    ZIO.serviceWithZIO[AccountService](_.closePeriod(fromPeriod, toPeriod, inStmtAccId, company))
}
