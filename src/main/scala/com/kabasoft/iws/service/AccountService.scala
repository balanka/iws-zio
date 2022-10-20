package com.kabasoft.iws.service

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait AccountService {
  def getBalance(accId: String, fromPeriod: Int, toPeriod: Int, companyId: String): ZIO[Any, RepositoryError, Account]
  def closePeriod(fromPeriod: Int, toPeriod: Int, inStmtAccId: String, company: String): ZIO[Any, RepositoryError, Int]
}

object AccountService {
  def getBalance(accId: String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[AccountService, RepositoryError, Account] =
    ZIO.service[AccountService].flatMap(_.getBalance(accId, fromPeriod, toPeriod, company))

  def closePeriod(
    fromPeriod: Int,
    toPeriod: Int,
    inStmtAccId: String,
    company: String
  ): ZIO[AccountService, RepositoryError, Int] =
    ZIO.service[AccountService].flatMap(_.closePeriod(fromPeriod, toPeriod, inStmtAccId, company))
}
