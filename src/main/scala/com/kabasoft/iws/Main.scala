package com.kabasoft.iws


import com.kabasoft.iws.api.AccountHttpRoutes.appAcc
import com.kabasoft.iws.api.FinancialsHttpRoutes.appFtr
import com.kabasoft.iws.api.JournalRoutes.appJournal
import com.kabasoft.iws.api.LoginRoutes.{appLogin, jwtDecode}
import com.kabasoft.iws.api.BankStmtEndpoint.appBankStmt
import .appCostcenter
import com.kabasoft.iws.api.PacEndpoint.appPac
import com.kabasoft.iws.api.BankEndpoint.appBank
import com.kabasoft.iws.api.CompanyEndpoint.appComp
import com.kabasoft.iws.api.ModuleEndpoint.appModule
import com.kabasoft.iws.api.SupplierEndpoint.appSup
import com.kabasoft.iws.api.CustomerEndpoint.appCust
import com.kabasoft.iws.api.UserEndpoint.appUser
import com.kabasoft.iws.api.VatEndpoint.appVat
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.domain.AppError
import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service._
import zio._
import zio.http.Middleware.bearerAuth
import zio.http.{Body, Http, Middleware, Request, Response}
import zio.http.model.{Headers, Status}
import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.middleware.Cors.CorsConfig
import zio.http.model.Method
import zio.http.{Server, ServerConfig}
import zio.sql.ConnectionPool

object Main extends ZIOAppDefault {


  val defaultX: ZLayer[Any, Throwable, Server] = {
    implicit val trace = Trace.empty
    ZLayer.succeed(ServerConfig()
      .leakDetection(LeakDetectionLevel.PARANOID)
      .binding("127.0.0.1", 9090)/*.port(8080)*/) >>> Server.live
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
    private val httpApp = wrap(appFtr) ++ masterfilesApp  ++ wrap(appPac) ++ wrap(appJournal) ++
      wrap(appCostcenter) ++ wrap(appBankStmt)++  expose
    val app= (appLogin  ++ httpApp)
      .mapError(e=>Response(Status.InternalServerError, Headers.empty, Body.fromString(e.getMessage)))
     //.withDefaultErrorResponse


  //val e:ErrorCallback = _=>ZIO.unit
   val run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.logInfo(s"Starting http server") *> //@@//@@ LogKeys.portKey(port)
     Server.serve(app @@ Middleware.cors(config) /*@@ZIO.addFinalizer(ZIO.logInfo("Shutting down http server"))*/)
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

   //}yield ()
}
