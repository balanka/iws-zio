package com.kabasoft.iws

import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.domain.BankStatement
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.{ BankStatementService, BankStatementServiceImpl }
import zio._
import zio.sql.ConnectionPool

object BankStmtImportApp extends ZIOAppDefault {

  val HEADER    = "Auftragskonto"
  val CHAR      = "\""
  val EXTENSION = ".CSV"
  val PATH      = "/Users/iwsmac/Downloads/import/bankStatement/43006329/202305/"
  val COMPANY   = "1000"

  def run = (for {
    bs <- BankStatementService.importBankStmt(PATH, HEADER, CHAR, EXTENSION, COMPANY, BankStatement.from)
    _  <- ZIO.debug(s"  BS ${bs}")
  } yield ())
    .provide(
      DbConfig.layer,
      DbConfig.connectionPoolConfig,
      ConnectionPool.live,
      BankStatementRepositoryImpl.live,
      BankStatementServiceImpl.live,
      TransactionRepositoryImpl.live,
      CustomerRepositoryImpl.live,
      SupplierRepositoryImpl.live,
      CompanyRepositoryImpl.live
    )
}
