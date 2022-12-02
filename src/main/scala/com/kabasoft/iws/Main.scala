package com.kabasoft.iws

import com.kabasoft.iws.api.AccountHttpRoutes.appAcc
import com.kabasoft.iws.api.CustomerRoutes.{appComp, appCust}
import com.kabasoft.iws.api.FinancialsHttpRoutes.appFtr
import com.kabasoft.iws.api.JournalRoutes.appJournal
import com.kabasoft.iws.api.LoginRoutes.{appLogin, jwtDecode}
import com.kabasoft.iws.api.MasterfilesHttpRoutes._
import com.kabasoft.iws.api.PacHttpRoutes.appPac
import com.kabasoft.iws.api.SupplierRoutes.appSup
import com.kabasoft.iws.api.VatHttpRoutes.appVat
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.domain.AppError
import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service._
import zio._
import zio.http.Middleware.bearerAuth
import zio.http._
import zio.http.api.Middleware.cors
import zio.http.middleware.Cors.CorsConfig
import zio.http.model.Method
import zio.sql.ConnectionPool

object Main extends ZIOAppDefault {


  val defaultX: ZLayer[Any, Throwable, Server] = {
    implicit val trace = Trace.empty
    ZLayer.succeed(ServerConfig().binding("127.0.0.1", 8090).port(8080)) >>> Server.live
  }

  val config: CorsConfig =
    CorsConfig(
      anyOrigin = true,
      anyMethod = false,
      allowedOrigins = s => s.equals("localhost")||s.equals("127.0.0.1"),
      allowedMethods = Some(Set(Method.GET, Method.POST))
    )
def wrap[R](app: Http[R, AppError.RepositoryError, Request, Response] ) =  app@@ bearerAuth(jwtDecode(_).isDefined)

  val masterfilesApp =   wrap(appAcc)++ wrap(appBank) ++ wrap(appModule) ++ wrap(appCust) ++ wrap(appSup) ++
                       wrap(appComp) ++ wrap(appBankStmt)++ wrap(appVat)++wrap(appUser)
  val httpApp = wrap(appFtr) ++ masterfilesApp ++ wrap(appPac) ++ wrap(appJournal) ++
    wrap(appCostcenter) ++ wrap(appBankStmt)++  expose
  override val run = Server.serve((appLogin++ httpApp )@@  Middleware.cors(config))
    .provide(
    defaultX,
    connectionPoolConfig,
    DbConfig.layer,
    ConnectionPool.live,
    AccountServiceImpl.live,
    AccountRepositoryImpl.live,
    CompanyRepositoryImpl.live,
    CostcenterRepositoryImpl.live,
    CustomerRepositoryImpl.live,
    SupplierRepositoryImpl.live,
    BankRepositoryImpl.live,
    ModuleRepositoryImpl.live,
    BankStatementRepositoryImpl.live,
    TransactionRepositoryImpl.live,
    PacRepositoryImpl.live,
    UserRepositoryImpl.live,
    VatRepositoryImpl.live,
    JournalRepositoryImpl.live,
    FinancialsServiceImpl.live)
}
