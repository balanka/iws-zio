package com.kabasoft.iws

import com.kabasoft.iws.api.AccountEndpoint.appAcc
import com.kabasoft.iws.api.ArticleEndpoint.appArticle
import com.kabasoft.iws.api.AssetEndpoint.appAsset
import com.kabasoft.iws.api.BankStmtEndpoint.appBankStmt
import com.kabasoft.iws.api.JournalEndpoint.appJournal
import com.kabasoft.iws.api.LoginRoutes.appLogin
import com.kabasoft.iws.api.Utils
import com.kabasoft.iws.api.PacEndpoint.appPac
import com.kabasoft.iws.api.CompanyEndpoint.appComp
import com.kabasoft.iws.api.CustomerEndpoint.appCust
import com.kabasoft.iws.api.EmployeeEndpoint.routesEmp
import com.kabasoft.iws.api.FModuleEndpoint.appFModule
import com.kabasoft.iws.api.FinancialsEndpoint.appFtr
import com.kabasoft.iws.api.ImportFileEndpoint.appImportFile
import com.kabasoft.iws.api.MasterfileEndpoint.appMasterfile
import com.kabasoft.iws.api.ModuleEndpoint.appModule
import com.kabasoft.iws.api.PayrollEndpoint.appPayroll
import com.kabasoft.iws.api.PermissionEndpoint.appPerm
import com.kabasoft.iws.api.RoleEndpoint.appRole
import com.kabasoft.iws.api.SalaryItemEndpoint.appSalaryItem
import com.kabasoft.iws.api.PayrollTaxRangeEndpoint.appPayrollTaxRange
import com.kabasoft.iws.api.StockEndpoint.appStock
import com.kabasoft.iws.api.StoreEndpoint.appStore
import com.kabasoft.iws.api.SupplierEndpoint.appSup
import com.kabasoft.iws.api.TransactionEndpoint.appLtr
import com.kabasoft.iws.api.UserEndpoint.appUser
import com.kabasoft.iws.api.VatEndpoint.appVat
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.config.{DbConfig, ServerConfig}
import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service._
import zio._
import zio.http.Header.{AccessControlAllowMethods, AccessControlAllowOrigin, Origin}
import zio.http.HttpAppMiddleware.{bearerAuth, cors}
import zio.http.Server.Config
import zio.http.internal.middlewares.Cors.CorsConfig
import zio.http.{Method, Server}
import zio.sql.ConnectionPool

import java.lang.System
import java.time.Clock
import java.util
import scala.annotation.nowarn
import scala.language.postfixOps

object IwsApp extends ZIOAppDefault {

  implicit val clock: Clock = Clock.systemUTC
  val env: util.Map[String, String] = System.getenv()
  println("env>>>" + env)
  val hostName: String =  if(env.get("IWS_API_HOST").trim.isEmpty) "0.0.0.0" else env.get("IWS_API_HOST")
  val port: Int =  if(env.get("IWS_API_PORT").trim.isEmpty) 8080 else env.get("IWS_API_PORT").toInt
  println("hostName>>> " + hostName + " hostport >>>" + port)
  private val serverLayer: ZLayer[Any, Throwable, Server] = {
    implicit val trace: Trace = Trace.empty
    ZLayer.succeed(
      Config.default.binding(hostName, port)
    ) >>> Server.live
  }
  val config: CorsConfig =
    CorsConfig(

      allowedOrigin = {
        case origin @ Origin.Value(_, host, _) if (host == hostName ||
          host == "localhost" || host == "127.0.0.1" ) => Some(AccessControlAllowOrigin.Specific(origin))
        case _ => None
      },
      allowedMethods = AccessControlAllowMethods(Method.GET, Method.POST, Method.PUT, Method.PATCH, Method.DELETE)
    )

  private val httpApp =   (appVat ++ appSup ++ appCust ++ appModule ++ appAcc  ++ appComp  ++ appFtr ++ appLtr ++appFModule
    ++ routesEmp ++ appArticle ++ appStore ++ appSalaryItem ++ appPayroll ++ appMasterfile ++appPayrollTaxRange
    ++ appImportFile ++appBankStmt ++  appUser ++ appPac ++ appJournal  ++appPerm ++ appRole ++ appAsset ++ appStock)

  @nowarn val run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =

    ZIO.logInfo(s"Starting http server") *>
      Server
        .serve((appLogin++expose.toApp).withDefaultErrorResponse ++httpApp.toApp@@ bearerAuth(Utils.jwtDecode(_).isDefined)@@cors(config) /*@@ZIO.addFinalizer(ZIO.logInfo("Shutting down http server"))*/ )
        .provide(
          serverLayer,
          connectionPoolConfig,
          DbConfig.layer,
          ServerConfig.layer,
          ConnectionPool.live,
          ArticleCacheImpl.live,
          ArticleRepositoryImpl.live,
          AssetCacheImpl.live,
          AssetRepositoryImpl.live,
          AssetsServiceImpl.live,
          AccountServiceImpl.live,
          AccountCacheImpl.live,
          AccountRepositoryImpl.live,
          CompanyRepositoryImpl.live,
          ImportFileCacheImpl.live,
          CustomerRepositoryImpl.live,
          CustomerCacheImpl.live,
          PayrollTaxRangeRepositoryImpl.live,
          PayrollTaxRangeCacheImpl.live,
          EmployeeRepositoryImpl.live,
          EmployeeCacheImpl.live,
          EmployeeServiceImpl.live,
          MasterfileRepositoryImpl.live,
          MasterfileCacheImpl.live,
          SupplierRepositoryImpl.live,
          SupplierCacheImpl.live,
          StoreRepositoryImpl.live,
          StoreCacheImpl.live,
          StockRepositoryImpl.live,
          ImportFileRepositoryImpl.live,
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
          FinancialsTransactionRepositoryImpl.live,
          PacRepositoryImpl.live,
          UserRepositoryImpl.live,
          VatRepositoryImpl.live,
          VatCacheImpl.live,
          SalaryItemRepositoryImpl.live,
          SalaryItemCacheImpl.live,
          JournalRepositoryImpl.live,
          BankStatementServiceImpl.live,
          FinancialsServiceImpl.live,
          PostFinancialsTransactionRepositoryImpl.live,
          PostTransactionRepositoryImpl.live,
          TransactionRepositoryImpl.live,
          TransactionCacheImpl.live,
          TransactionServiceImpl.live,
          TransactionLogRepositoryImpl.live,
          PostOrderImpl.live,
          PostGoodreceivingImpl.live,
          PostBillOfDeliveryImpl.live,
          PostCustomerInvoiceImpl.live,
          PostSupplierInvoiceImpl.live
        ).<*( ZIO.logInfo(s"http server started successfully!!!!"))
}
