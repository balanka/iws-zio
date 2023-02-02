package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankStatement
import zio.ZIO

trait BankStatementService {

  def importBankStmt(
    path: String,
    header: String,
    char: String,
    extension: String,
    company: String,
    buildFn: String => BankStatement
  ): ZIO[Any, RepositoryError, Int]
  def postAll(ids: List[Long], companyId: String) : ZIO[Any, RepositoryError, Int]

}
object BankStatementService {

  def postAll(ids: List[Long], companyId: String) : ZIO[BankStatementService, RepositoryError, Int] =
    ZIO.service[BankStatementService].flatMap(_.postAll(ids, companyId))

  def importBankStmt(
    path: String,
    header: String,
    char: String,
    extension: String,
    company: String,
    buildFn: String => BankStatement
  ): ZIO[BankStatementService, RepositoryError, Int] =
    ZIO.service[BankStatementService].flatMap(_.importBankStmt(path, header, char, extension, company, buildFn))
}
