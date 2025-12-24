package com.kabasoft.iws.service

import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._

trait AccountService:
  def getBalance(accId: String, toPeriod: Int, companyId: String): ZIO[Any, RepositoryError, List[Account]]
  def closePeriod(toPeriod: Int, inStmtAccId: String, company: String): ZIO[Any, RepositoryError, Int]


object AccountService:
  def getBalance(accId: String, toPeriod: Int, company: String): ZIO[AccountService, RepositoryError, List[Account]] =
    ZIO.service[AccountService].flatMap(_.getBalance(accId,  toPeriod, company))

  def closePeriod(toPeriod: Int, inStmtAccId: String, company: String
  ): ZIO[AccountService, RepositoryError, Int] =
    ZIO.service[AccountService].flatMap(_.closePeriod( toPeriod, inStmtAccId, company))

