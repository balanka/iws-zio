package com.kabasoft.iws

import com.kabasoft.iws.config.AppConfig
import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.resources.AppResources
import zio.interop.catz.*
import cats.effect.std.Console
import natchez.Trace.Implicits.noop
import com.kabasoft.iws.domain.BankStatement
import com.kabasoft.iws.repository.*
import com.kabasoft.iws.service.{BankStatementService, BankStatementServiceLive}
import zio.*

import java.lang.System
import java.util

object BankStmtImportApp extends ZIOAppDefault {
  val env: util.Map[String, String] = System.getenv()
  private val PATH = if(env.keySet().contains("IWS_IMPORT_DIR"))
    env.get("IWS_IMPORT_DIR")
  else "/Users/iwsmac/Downloads/import/bankStatement/43006329/202312/" //"/Users/iwsmac/Downloads/import/bankStatement/43006329/2023-09-10-11/"
  private val HEADER    = "Auftragskonto"
  private val CHAR      = "\""
  private val EXTENSION = ".CSV"
  val COMPANY   = "1000"

  given Console[Task] = Console.make[Task]
  def run: ZIO[Any & Scope, Any, Any] = {
    val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
      for {
        config <- ZIO.service[AppConfig]
        res <- AppResources.make(config).toScopedZIO
      } yield res
    )
    (for {
      bs <- BankStatementService.importBankStmt(PATH, HEADER, CHAR, EXTENSION, COMPANY, BankStatement.from)
      _ <- ZIO.debug(s"  BS ${bs}")
    } yield ())
      .provide(
        appResourcesL.project(_.postgres),
        appConfig,
        BankAccountRepositoryLive.live,
        BankStatementRepositoryLive.live,
        BankStatementServiceLive.live,
        FinancialsTransactionRepositoryLive.live,
        CustomerRepositoryLive.live,
        SupplierRepositoryLive.live,
        VatRepositoryLive.live,
        AccountRepositoryLive.live,
        CompanyRepositoryLive.live
      )
  }
}
