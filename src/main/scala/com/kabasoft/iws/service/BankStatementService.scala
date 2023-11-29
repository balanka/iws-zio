package com.kabasoft.iws.service

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankStatement
import zio.ZIO

trait BankStatementService {

  def importBankStmt( path: String,
                      header: String,
                      char: String,
                      extension: String,
                      company: String,
                      buildFn: String => BankStatement
  ): ZIO[Any, RepositoryError, Int]
  def post(id: Long, companyId:String): ZIO[Any, RepositoryError, BankStatement]
  def post(ids: List[Long], companyId:String): ZIO[Any, RepositoryError, List[BankStatement]]

}
object BankStatementService {

  def post(id: Long, companyId:String): ZIO[BankStatementService, RepositoryError, BankStatement] =
    ZIO.service[BankStatementService].flatMap(_.post(id, companyId))
  def post(ids: List[Long], companyId:String): ZIO[BankStatementService, RepositoryError, List[BankStatement]] =
    ZIO.service[BankStatementService].flatMap(_.post(ids, companyId))
  def importBankStmt( path: String,
                      header: String,
                      char: String,
                      extension: String,
                      company: String,
                      buildFn: String => BankStatement
  ): ZIO[BankStatementService, RepositoryError, Int] =
    ZIO.service[BankStatementService].flatMap(_.importBankStmt(path, header, char, extension, company, buildFn))
}
