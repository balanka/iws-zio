package com.kabasoft.iws

import com.kabasoft.iws.api.AccountEndpoint.appAcc
import com.kabasoft.iws.api.ArticleEndpoint.appArticle
import com.kabasoft.iws.api.AssetEndpoint.appAsset
import com.kabasoft.iws.api.JournalEndpoint.appJournal
import com.kabasoft.iws.api.LoginRoutes.appLogin
import com.kabasoft.iws.api.BankStmtEndpoint.appBankStmt
import com.kabasoft.iws.api.Utils
//import com.kabasoft.iws.api.CostcenterEndpoint.appCC
import com.kabasoft.iws.api.PacEndpoint.appPac
//import com.kabasoft.iws.api.BankEndpoint.appBank
import com.kabasoft.iws.api.MasterfileEndpoint.appMasterfile
import com.kabasoft.iws.api.CompanyEndpoint.appComp
import com.kabasoft.iws.api.ModuleEndpoint.appModule
import com.kabasoft.iws.api.SupplierEndpoint.appSup
import com.kabasoft.iws.api.CustomerEndpoint.appCust
import com.kabasoft.iws.api.EmployeeEndpoint.routesEmp
import com.kabasoft.iws.api.FModuleEndpoint.appFModule
import com.kabasoft.iws.api.FinancialsEndpoint.appFtr
import com.kabasoft.iws.api.ImportFileEndpoint.appImportFile
import com.kabasoft.iws.api.PayrollEndpoint.appPayroll
import com.kabasoft.iws.api.PermissionEndpoint.appPerm
import com.kabasoft.iws.api.RoleEndpoint.appRole
import com.kabasoft.iws.api.SalaryItemEndpoint.appSalaryItem
import com.kabasoft.iws.api.StoreEndpoint.appStore
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
  val hostName:String =  env.get("IWS_API_HOST") //else "0.0.0.0"
  val port:Int = env.get("IWS_API_PORT").toInt //else 8080
  //println("hostName>>>" + hostName)
  //println("hostport>>>" + port)
  private val serverLayer: ZLayer[Any, Throwable, Server] = {
    implicit val trace = Trace.empty
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

  private val httpApp =   (appVat ++ appSup ++ appCust ++ appModule ++ appAcc  ++ appComp  ++ appFtr ++ appFModule
    ++ routesEmp ++ appArticle ++ appStore ++ appSalaryItem ++ appPayroll ++ appMasterfile
    ++ appImportFile ++appBankStmt ++  appUser ++ appPac ++ appJournal  ++appPerm ++ appRole ++ appAsset)

  @nowarn val run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =

    ZIO.logInfo(s"Starting http server") *> // @@//@@ LogKeys.portKey(port)
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
          AccountServiceImpl.live,
          AccountCacheImpl.live,
          AccountRepositoryImpl.live,
          CompanyRepositoryImpl.live,
          //CostcenterRepositoryImpl.live,
          //CostcenterCacheImpl.live,
          ImportFileCacheImpl.live,
          CustomerRepositoryImpl.live,
          CustomerCacheImpl.live,
          EmployeeRepositoryImpl.live,
          EmployeeCacheImpl.live,
          MasterfileRepositoryImpl.live,
          MasterfileCacheImpl.live,
          SupplierRepositoryImpl.live,
          SupplierCacheImpl.live,
          StoreRepositoryImpl.live,
          StoreCacheImpl.live,
          //BankRepositoryImpl.live,
          ImportFileRepositoryImpl.live,
          //BankCacheImpl.live,
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
          PostTransactionRepositoryImpl.live,
          EmployeeServiceImpl.live
        )//.<*( ZIO.logInfo(s"http server started successfully!!!!"))
}
