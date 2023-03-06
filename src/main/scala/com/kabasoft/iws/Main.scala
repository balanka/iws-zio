package com.kabasoft.iws

import com.kabasoft.iws.api.AccountEndpoint.appAcc
import zio.http.Middleware.bearerAuth
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
import com.kabasoft.iws.api.UserEndpoint.appUser
import com.kabasoft.iws.api.VatEndpoint.appVat
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service._
import zio._
import zio.http.{Body, Middleware, Response}
import zio.http.model.{Headers, Status}

import java.time.Clock
//import zio.http.ServerConfig.LeakDetectionLevel
import zio.http.middleware.Cors.CorsConfig
import zio.http.model.Method
import zio.http.{Server, ServerConfig}
import zio.sql.ConnectionPool

object Main extends ZIOAppDefault {

  implicit val clock: Clock = Clock.systemUTC
  private val serverLayer: ZLayer[Any, Throwable, Server] = {
    implicit val trace = Trace.empty
    ZLayer.succeed(
      ServerConfig()
        //.leakDetection(LeakDetectionLevel.PARANOID)
        .binding("localhost", 8091) /*.port(8080)*/
    ) >>> Server.live
  }

  val config: CorsConfig =
    CorsConfig(
      anyOrigin = true,
      anyMethod = false,
      allowedHeaders = Some(Set("*")),
      allowedOrigins = s => s.equals("127.0.0.1:3000")||s.equals("http://127.0.0.1:3000") || s.equals("localhost:3000")|| s.equals("http://localhost:3000"),
      allowedMethods = Some(Set(Method.GET, Method.POST))
     // allowedHeaders = Some(
    //  Set(HttpHeaderNames.CONTENT_TYPE.toString, HttpHeaderNames.AUTHORIZATION.toString)
      //  HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString, "*"),

    )

  def decode(token:String) = {
    val x=jwtDecode(token).isDefined
    x
  }
 // def wrap[R](app: Http[R, AppError.RepositoryError, Request, Response]) = app @@ bearerAuth(decode)//jwtDecode(_).isDefined)

  //private val masterfilesApp  = wrap(appAcc) ++ wrap(appBank) ++ wrap(appModule) ++ wrap(appCust) ++ wrap(appSup) ++
   // wrap(appComp) ++ wrap(appBankStmt) ++ wrap(appVat) ++ wrap(appUser)
   val httpApp =  (appLogin ++ (appVat ++ appSup ++ appCust ++ appModule ++ appAcc ++ appBank  ++ appComp //++ appFtr
     ++ appBankStmt ++  appUser ++ appPac ++ appJournal ++ appCC ++ appBankStmt).toApp @@ bearerAuth(jwtDecode(_).isDefined) ++ expose)
     .mapError(e => Response(Status.InternalServerError, Headers.empty, Body.fromString(e.toString)))
 // val appx =  appBank++ appAcc ++appCC ++ appModule++ appCust ++ appSup ++ appComp++appBankStmt++appVat ++ appUser++
 //    appPac ++ appJournal ++appCC ++ appBankStmt ++ appFtr ++ expose
 //   .mapError(e => Response(Status.InternalServerError, Headers.empty, Body.fromString(e.getMessage)))
  //val app             = (appLogin ++ appCust++ appBank ++ appAcc ++ appCC ++ appModule  ++ appSup ++ appComp ++ appBankStmt ++ appVat ++ appUser ++
  //  appPac ++ appJournal ++ appCC ++ appBankStmt ++ appFtr ++ expose)/*@@ bearerAuth(decode))*/.map(r=>r.addHeader(Header("Access-Control-Allow-Origin", "*")))
   //val app = (appLogin ++httpApp)//.map(r=>r.addHeader(Header("Access-Control-Allow-Origin", "*")))
   //                                          .mapError(e => Response(Status.InternalServerError, Headers.empty, Body.fromString(e.toString)))
  // .withDefaultErrorResponse

  // val e:ErrorCallback = _=>ZIO.unit
  val run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    ZIO.logInfo(s"Starting http server") *> // @@//@@ LogKeys.portKey(port)
      Server
        .serve(httpApp @@Middleware.cors(config) /*@@ZIO.addFinalizer(ZIO.logInfo("Shutting down http server"))*/ )
        .provide(
          serverLayer,
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
          //TransactionRepositoryImpl.live,
          PacRepositoryImpl.live,
          UserRepositoryImpl.live,
          VatRepositoryImpl.live,
          JournalRepositoryImpl.live,
         // FinancialsServiceImpl.live
        )
}
