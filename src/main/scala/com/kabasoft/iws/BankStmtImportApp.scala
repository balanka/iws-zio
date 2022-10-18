package com.kabasoft.iws

import zio._
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.domain.BankStatement
import com.kabasoft.iws.repository.{BankStatementRepositoryImpl, CompanyRepositoryImpl, CustomerRepositoryImpl, SupplierRepositoryImpl, TransactionRepositoryImpl}
import com.kabasoft.iws.service.{ BankStatementService, BankStatementServiceImpl}
import zio.sql.ConnectionPool

object BankStmtImportApp extends ZIOAppDefault {

  val HEADER    = "Auftragskonto"
  val CHAR      = "\""
  val EXTENSION = ".CSV"
  val PATH      = "/Users/iwsmac/Downloads/import/bankStatement/43006329/"
  val COMPANY   = "1000"

  def run = (for {
    bs <- BankStatementService.importBankStmt(PATH, HEADER, CHAR, EXTENSION, COMPANY, BankStatement.from)
    _  <- ZIO.debug(s"  BS ${bs}")
  } yield ())
    .provide(DbConfig.layer, ConnectionPool.live, DbConfig.connectionPoolConfig,
      BankStatementRepositoryImpl.live, BankStatementServiceImpl.live,
      TransactionRepositoryImpl.live, CustomerRepositoryImpl.live,
      SupplierRepositoryImpl.live, CompanyRepositoryImpl.live)
}
