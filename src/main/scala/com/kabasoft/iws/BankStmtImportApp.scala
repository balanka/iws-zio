package com.kabasoft.iws

import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.domain.BankStatement
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.{ BankStatementService, BankStatementServiceImpl }
import zio._
import zio.sql.ConnectionPool
import java.lang.System

object BankStmtImportApp extends ZIOAppDefault {
  ;
  val env = System.getenv()
  val PATH = if(env.keySet().contains("IWS_IMPORT_DIR"))
    env.get("IWS_IMPORT_DIR")
  else "/Users/iwsmac/Downloads/import/bankStatement/43006329/202104_12/"
  val HEADER    = "Auftragskonto"
  val CHAR      = "\""
  val EXTENSION = ".CSV"
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
      FinancialsTransactionRepositoryImpl.live,
      CustomerRepositoryImpl.live,
      SupplierRepositoryImpl.live,
      VatRepositoryImpl.live,
      AccountRepositoryImpl.live,
      CompanyRepositoryImpl.live
    )
}
