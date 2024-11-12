package com.kabasoft.iws
import com.kabasoft.iws.healthcheck.expose
import zio.{Console as ZConsole, *}
import com.kabasoft.iws.api.LoginRoutes.loginRoutes
import com.kabasoft.iws.api.Utils
import com.kabasoft.iws.config.AppConfig
import com.kabasoft.iws.resources.AppResources
import zio.interop.catz.*
import cats.effect.std.Console
import natchez.Trace.Implicits.noop
import com.kabasoft.iws.config.appConfig
import com.kabasoft.iws.api.AccountEndpoint.AccRoutes
import com.kabasoft.iws.api.ArticleEndpoint.articleRoutes
import com.kabasoft.iws.api.AssetEndpoint.assetRoutes
import com.kabasoft.iws.api.BankStmtEndpoint.bankStmtRoutes
import com.kabasoft.iws.api.JournalEndpoint.journalRoutes
import com.kabasoft.iws.api.PacEndpoint.pacRoutes
import com.kabasoft.iws.api.CompanyEndpoint.companyRoutes
import com.kabasoft.iws.api.CustomerEndpoint.customerRoutes
import com.kabasoft.iws.api.EmployeeEndpoint.employeeRoutes
import com.kabasoft.iws.api.FModuleEndpoint.fmoduleRoutes
import com.kabasoft.iws.api.FinancialsEndpoint.financialsRoutes
import com.kabasoft.iws.api.ImportFileEndpoint.importFileRoutes
import com.kabasoft.iws.api.MasterfileEndpoint.masterfileRoutes
import com.kabasoft.iws.api.ModuleEndpoint.moduleRoutes
import com.kabasoft.iws.api.PayrollEndpoint.payrollRoutes
import com.kabasoft.iws.api.PermissionEndpoint.permissionRoutes
import com.kabasoft.iws.api.RoleEndpoint.roleRoutes
import com.kabasoft.iws.api.SalaryItemEndpoint.salaryItemRoutes
import com.kabasoft.iws.api.PayrollTaxRangeEndpoint.payrollTaxRoutes
import com.kabasoft.iws.api.StockEndpoint.stockRoutes
import com.kabasoft.iws.api.StoreEndpoint.storeRoutes
import com.kabasoft.iws.api.SupplierEndpoint.supplierRoutes
import com.kabasoft.iws.api.TransactionEndpoint.transactionRoutes
import com.kabasoft.iws.api.UserEndpoint.userRoutes
import com.kabasoft.iws.api.Utils.bearerAuthWithContext
import com.kabasoft.iws.api.VatEndpoint.vatRoutes
//import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
//import com.kabasoft.iws.config.{DbConfig, connectionPoolConfig}
//import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service._
import zio._
import zio.http.Header.{AccessControlAllowMethods, AccessControlAllowOrigin, Origin}
import zio.http.Middleware.{CorsConfig, cors}
//import zio.http.HttpAppMiddleware.{bearerAuth, cors}
import zio.http.Server.Config
//import zio.http.internal.middlewares.Cors.CorsConfig
import zio.http.{Method, Server}
//import zio.sql.ConnectionPool

import java.lang.System
import java.time.Clock
import java.util
import scala.annotation.nowarn
import scala.language.postfixOps

object IwsApp extends ZIOAppDefault {

  implicit val clock: Clock = Clock.systemUTC
  val env: util.Map[String, String] = System.getenv()
  //println("env>>>" + env)
  val hostName: String =  if(env.get("IWS_API_HOST").trim.isEmpty) "0.0.0.0" else env.get("IWS_API_HOST")
  val port: Int =  if(env.get("IWS_API_PORT").trim.isEmpty) 8080 else env.get("IWS_API_PORT").toInt
  println("hostName>>> " + hostName + " hostport >>>" + port)
  private val serverLayer: ZLayer[Any, Throwable, Server] = {
    implicit val trace: Trace = Trace.empty
    ZLayer.succeed(
      Config.default.binding(hostName, port)
    ) >>> Server.live
  }
 // val config: CorsConfig =
//    CorsConfig(
//      allowedOrigin = {
//        case origin if origin == Origin.parse("http://0.0.0.0:3000").toOption.get =>
//          Some(AccessControlAllowOrigin.Specific(origin))
//        case _ => None
//      },
//      allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.PUT, Method.PATCH, Method.DELETE)
//    )
  val config: CorsConfig =
    CorsConfig(
      allowedOrigin = {
        case origin @ Origin.Value(_, host, _) if (host == hostName ||
          host == "localhost" || host == "127.0.0.1" ) => Some(AccessControlAllowOrigin.Specific(origin))
        case _ => None
      },
      allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.PUT, Method.PATCH, Method.DELETE)
    )

  private val httpApp = (AccRoutes++assetRoutes ++ supplierRoutes++ customerRoutes ++ moduleRoutes ++ companyRoutes 
   ++ bankStmtRoutes++transactionRoutes ++fmoduleRoutes++employeeRoutes++articleRoutes++salaryItemRoutes
   ++ importFileRoutes++payrollRoutes++pacRoutes++journalRoutes++payrollRoutes++masterfileRoutes++stockRoutes
   ++ userRoutes++permissionRoutes++payrollTaxRoutes++ financialsRoutes++roleRoutes++vatRoutes++storeRoutes)
  
  @nowarn val run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    given Console[Task] = Console.make[Task]
    val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
      for
        config <- ZIO.service[AppConfig]
        res    <- AppResources.make(config).toScopedZIO
      yield res
    )
    ZIO.logInfo(s"Starting http server") *>
      Server
        //.serve((appLogin++expose.toApp).withDefaultErrorResponse ++httpApp.toApp@@ bearerAuth(Utils.jwtDecode(_).isDefined)@@cors(config) 
        .serve((loginRoutes++expose++httpApp@@bearerAuthWithContext)@@cors(config))
        //.serve(loginRoutes++expose++httpApp@@bearerAuthWithContext)
        .provide(
          serverLayer,
          appResourcesL.project(_.postgres),
          appResourcesL,
          appConfig,
          ArticleRepositoryLive.live,
          AssetRepositoryLive.live,
          AssetsServiceLive.live,
          AccountServiceLive.live,
          AccountRepositoryLive.live,
          BankAccountRepositoryLive.live,
          CompanyRepositoryLive.live,
          CustomerRepositoryLive.live,
          PayrollTaxRangeRepositoryLive.live,
          EmployeeRepositoryLive.live,
          EmployeeServiceLive.live,
          MasterfileRepositoryLive.live,
          SupplierRepositoryLive.live,
          StoreRepositoryLive.live,
          StockRepositoryLive.live,
          ImportFileRepositoryLive.live,
          ModuleRepositoryLive.live,
          FModuleRepositoryLive.live,
          RoleRepositoryLive.live,
          PermissionRepositoryLive.live,
          BankStatementRepositoryLive.live,
          FinancialsTransactionRepositoryLive.live,
          PacRepositoryLive.live,
          UserRepositoryLive.live,
          VatRepositoryLive.live,
          SalaryItemRepositoryLive.live,
          JournalRepositoryLive.live,
          BankStatementServiceLive.live,
          FinancialsServiceLive.live,
          //PostFinancialsTransactionRepositoryLive.live,
          //PostTransactionRepositoryLive.live,
          TransactionRepositoryLive.live,
          TransactionServiceLive.live,
          //TransactionLogRepositoryLive.live,
          PostOrderLive.live,
          PostSalesOrderLive.live,
          PostGoodreceivingLive.live,
          PostBillOfDeliveryLive.live,
          PostCustomerInvoiceLive.live,
          PostSupplierInvoiceLive.live
        ).<*( ZIO.logInfo(s"http server started successfully!!!!"))
}
