package com.kabasoft.iws

import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.domain.BankStatement
import com.kabasoft.iws.repository.{BankStatementRepository, BankStatementRepositoryImpl}
import com.kabasoft.iws.service.BankStmtImportService
import zio.sql.ConnectionPool
import zio._

object BankStmtImportApp extends ZIOAppDefault {

  val HEADER = "Auftragskonto"
  val CHAR = "\""
  val EXTENSION = ".CSV"
  val PATH = "/Users/iwsmac/Downloads/import/bankStatement/43006329/"

  def run = (for {
    bs <- BankStmtImportService.importFromPath(PATH, HEADER, CHAR, EXTENSION, BankStatement.from)
    i <- BankStatementRepository.create(bs)
    _ <- ZIO.debug(s" inserted $i  record")
  } yield ())
    .provide(DbConfig.layer, ConnectionPool.live, DbConfig.connectionPoolConfig, BankStatementRepositoryImpl.live)
}
