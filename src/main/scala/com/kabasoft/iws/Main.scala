package com.kabasoft.iws

import com.kabasoft.iws.api.AccountEndpoint.appAcc
import com.kabasoft.iws.api.AssetEndpoint.appAsset
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
import com.kabasoft.iws.api.FModuleEndpoint.appFModule
import com.kabasoft.iws.api.FinancialsEndpoint.appFtr
import com.kabasoft.iws.api.ImportFileEndpoint.appImportFile
import com.kabasoft.iws.api.PermissionEndpoint.appPerm
import com.kabasoft.iws.api.RoleEndpoint.appRole
import com.kabasoft.iws.api.UserEndpoint.appUser
import com.kabasoft.iws.api.VatEndpoint.appVat
import com.kabasoft.iws.config.{DbConfig, ServerConfig}
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
import java.lang.System


object Main extends ZIOAppDefault {

  implicit val clock: Clock = Clock.systemUTC
  val env = System.getenv()
  val hostName = if (env.keySet().contains("IWS_API_HOST")) env.get("IWS_API_HOST") else "0.0.0.0"
  val port = if (env.keySet().contains("IWS_API_PORT")) env.get("IWS_API_PORT").toInt else 8080
  println("hostName>>>" + hostName)
  println("hostport>>>" + port)
  private val serverLayer: ZLayer[Any, Throwable, Server] = {
    implicit val trace = Trace.empty
    ZLayer.succeed(
      Config.default.binding(hostName, port)
    ) >>> Server.live
  }
  val config: CorsConfig =
    CorsConfig(

      allowedOrigin = {
        case origin @ Origin.Value(_, host, _) if (host == "iwsmacs-MacBook-Pro.local" || host == "mac-studio.fritz.box" ||
          host == hostName ||
          host == "localhost" || host == "127.0.0.1") => Some(AccessControlAllowOrigin.Specific(origin))
        case _ => None
      },
      allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.PUT, Method.PATCH, Method.DELETE)
    )

  val httpApp =   (appVat ++ appSup ++ appCust ++ appModule ++ appAcc ++ appBank  ++ appComp  ++ appFtr ++ appFModule
     ++ appImportFile ++appBankStmt ++  appUser ++ appPac ++ appJournal ++ appCC ++ appBankStmt ++appPerm ++ appRole ++ appAsset)// ++expose)//.toApp.withDefaultErrorResponse @@ bearerAuth(jwtDecode(_).isDefined)

  @nowarn val run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =

    ZIO.logInfo(s"Starting http server") *> // @@//@@ LogKeys.portKey(port)
      Server
        .serve((appLogin++expose.toApp).withDefaultErrorResponse ++httpApp.toApp@@ bearerAuth(jwtDecode(_).isDefined)@@cors(config) /*@@ZIO.addFinalizer(ZIO.logInfo("Shutting down http server"))*/ )
        .provide(
          serverLayer,
          connectionPoolConfig,
          DbConfig.layer,
          ServerConfig.layer,
          ConnectionPool.live,
          AssetCacheImpl.live,
          AssetRepositoryImpl.live,
          AccountServiceImpl.live,
          AccountCacheImpl.live,
          AccountRepositoryImpl.live,
          CompanyRepositoryImpl.live,
          CostcenterRepositoryImpl.live,
          CostcenterCacheImpl.live,
          ImportFileCacheImpl.live,
          CustomerRepositoryImpl.live,
          CustomerCacheImpl.live,
          SupplierRepositoryImpl.live,
          SupplierCacheImpl.live,
          BankRepositoryImpl.live,
          ImportFileRepositoryImpl.live,
          BankCacheImpl.live,
          ModuleRepositoryImpl.live,
          ModuleCacheImpl.live,
          FModuleRepositoryImpl.live,
          FModuleCacheImpl.live,
          RoleRepositoryImpl.live,
          RoleCacheImpl.live,
          PermissionRepositoryImpl.live,
          PermissionCacheImpl.live,
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
        )//.<*( ZIO.logInfo(s"http server started successfully!!!!"))
}
