package com.kabasoft.iws

import com.kabasoft.iws.api.AccountEndpoint.appAcc
import com.kabasoft.iws.api.JournalEndpoint.appJournal
import com.kabasoft.iws.api.LoginRoutes.{appLogin, jwtDecode}
import com.kabasoft.iws.api.BankStmtEndpoint.appBankStmt
import com.kabasoft.iws.api.CostcenterEndpoint.appCC
import com.kabasoft.iws.api.PacEndpoint.appPac
import com.kabasoft.iws.api.BankEndpoint.appBank
import com.kabasoft.iws.api.CompanyEndpoint.appComp
import com.kabasoft.iws.api.ModuleEndpoint.appModule
import com.kabasoft.iws.api.SupplierEndpoint.appSup
import com.kabasoft.iws.api.CustomerEndpoint.appCust
import com.kabasoft.iws.api.FinancialsEndpoint.appFtr
import com.kabasoft.iws.api.UserEndpoint.appUser
import com.kabasoft.iws.api.VatEndpoint.appVat
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service._
import zio._
import zio.http.Header.{AccessControlAllowMethods, AccessControlAllowOrigin, Origin}
import zio.http.HttpAppMiddleware.{bearerAuth, cors}

import java.time.Clock
import zio.http.internal.middlewares.Cors.CorsConfig
import zio.http.{Method, Server}
import zio.http.Server.Config
import zio.sql.ConnectionPool
import scala.annotation.nowarn


object Main extends ZIOAppDefault {

  implicit val clock: Clock = Clock.systemUTC

  private val serverLayer: ZLayer[Any, Throwable, Server] = {
    implicit val trace = Trace.empty
    ZLayer.succeed(
      Config.default.binding("mac-studio.fritz.box", 8091)
    ) >>> Server.live
  }
  val config: CorsConfig =
    CorsConfig(

      allowedOrigin = {
        case origin @ Origin.Value(_, host, _) if (host == "iwsmacs-MacBook-Pro.local" || host == "mac-studio.fritz.box" ||
          host == "localhost" || host == "127.0.0.1") => Some(AccessControlAllowOrigin.Specific(origin))
        case _ => None
      },
      allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.PUT, Method.PATCH, Method.DELETE)
    )

  val httpApp =   (appVat ++ appSup ++ appCust ++ appModule ++ appAcc ++ appBank  ++ appComp  ++ appFtr
     ++ appBankStmt ++  appUser ++ appPac ++ appJournal ++ appCC ++ appBankStmt ++expose)//.toApp.withDefaultErrorResponse @@ bearerAuth(jwtDecode(_).isDefined)

  @nowarn val run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.logInfo(s"Starting http server") *> // @@//@@ LogKeys.portKey(port)
      Server
        .serve((appLogin).withDefaultErrorResponse ++httpApp.toApp@@ bearerAuth(jwtDecode(_).isDefined)@@cors(config) /*@@ZIO.addFinalizer(ZIO.logInfo("Shutting down http server"))*/ )
        .provide(
          serverLayer,
          connectionPoolConfig,
          DbConfig.layer,
          ConnectionPool.live,
          AccountServiceImpl.live,
          AccountCacheImpl.live,
          AccountRepositoryImpl.live,
          CompanyRepositoryImpl.live,
          CostcenterRepositoryImpl.live,
          CostcenterCacheImpl.live,
          CustomerRepositoryImpl.live,
          CustomerCacheImpl.live,
          SupplierRepositoryImpl.live,
          SupplierCacheImpl.live,
          BankRepositoryImpl.live,
          BankCacheImpl.live,
          ModuleRepositoryImpl.live,
          ModuleCacheImpl.live,
          BankStatementRepositoryImpl.live,
          FinancialsTransactionCacheImpl.live,
          TransactionRepositoryImpl.live,
          PacRepositoryImpl.live,
          UserRepositoryImpl.live,
          VatRepositoryImpl.live,
          VatCacheImpl.live,
          JournalRepositoryImpl.live,
          BankStatementServiceImpl.live,
          FinancialsServiceImpl.live,
          PostTransactionRepositoryImpl.live
        )
}
