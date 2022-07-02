package com.kabasoft.iws.service
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio.ZIO

trait BankStmtService {

  def importBankStmt(
    path: String,
    header: String,
    char: String,
    extension: String,
    company: String,
    buildFn: String => BankStatement
  ): ZIO[Any, RepositoryError, Int]
}
object BankStmtService {
  def importBankStmt(
    path: String,
    header: String,
    char: String,
    extension: String,
    company: String,
    buildFn: String => BankStatement
  ): ZIO[BankStmtService, RepositoryError, Int] =
    ZIO.service[BankStmtService].flatMap(_.importBankStmt(path, header, char, extension, company, buildFn))
}
