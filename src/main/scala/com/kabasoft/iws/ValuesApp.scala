package com.kabasoft.iws

import com.kabasoft.iws.api.AssetEndpoint.assetRoutes
import com.kabasoft.iws.healthcheck.expose
import zio.{Trace, Console as ZConsole, *}
//import com.kabasoft.iws.api.LoginRoutes.appLogin
import cats.effect.std.Console
import com.kabasoft.iws.api.AccountEndpoint.AccRoutes
import com.kabasoft.iws.api.ArticleEndpoint.articleRoutes
import com.kabasoft.iws.api.AssetEndpoint.assetRoutes
import com.kabasoft.iws.api.BankStmtEndpoint.bankStmtRoutes
import com.kabasoft.iws.api.CompanyEndpoint.companyRoutes
import com.kabasoft.iws.api.CustomerEndpoint.customerRoutes
import com.kabasoft.iws.api.EmployeeEndpoint.employeeRoutes
import com.kabasoft.iws.api.FModuleEndpoint.fmoduleRoutes
import com.kabasoft.iws.api.FinancialsEndpoint.financialsRoutes
import com.kabasoft.iws.api.ImportFileEndpoint.importFileRoutes
import com.kabasoft.iws.api.JournalEndpoint.journalRoutes
import com.kabasoft.iws.api.LoginRoutes.loginRoutes
import com.kabasoft.iws.api.MasterfileEndpoint.masterfileRoutes
import com.kabasoft.iws.api.ModuleEndpoint.moduleRoutes
import com.kabasoft.iws.api.PacEndpoint.pacRoutes
import com.kabasoft.iws.api.PayrollEndpoint.payrollRoutes
import com.kabasoft.iws.api.PayrollTaxRangeEndpoint.payrollTaxRoutes
import com.kabasoft.iws.api.PermissionEndpoint.permissionRoutes
import com.kabasoft.iws.api.RoleEndpoint.roleRoutes
import com.kabasoft.iws.api.SalaryItemEndpoint.salaryItemRoutes
import com.kabasoft.iws.api.StockEndpoint.stockRoutes
import com.kabasoft.iws.api.StoreEndpoint.storeRoutes
import com.kabasoft.iws.api.SupplierEndpoint.supplierRoutes
import com.kabasoft.iws.api.TransactionEndpoint.transactionRoutes
import com.kabasoft.iws.api.UserEndpoint.userRoutes
import com.kabasoft.iws.api.Utils
import com.kabasoft.iws.api.VatEndpoint.vatRoutes
import com.kabasoft.iws.config.{AppConfig, appConfig}
import com.kabasoft.iws.resources.AppResources
import natchez.Trace.Implicits.noop
import zio.interop.catz.*
import com.kabasoft.iws.repository.*
import com.kabasoft.iws.service.*
import zio.{Console as ZConsole, *}
import zio.*
import zio._
import zio.http.Header.{AccessControlAllowMethods, AccessControlAllowOrigin, Origin}
import zio.http.Middleware.{CorsConfig, cors}
import zio.http.Server.Config
import cats.effect._
import cats.syntax.all._
//import org.typelevel.otel4s.trace.Tracer
import skunk._
import skunk.implicits._
import skunk.codec.all._
import java.lang.System
import java.time.Clock
import java.util
import scala.annotation.nowarn
import scala.language.postfixOps

//object ValuesApp extends ZIOAppDefault {

  //implicit val tracer: Tracer[IO] = Tracer.noop
//  implicit val trace: Trace = Trace.empty
//
//  val session: Resource[IO, Session[IO]] =
//    Session.single(
//      host     = "localhost",
//      user     = "jimmy",
//      database = "world",
//      password = Some("banana"),
//    )

//  case class Data(n: Int, s: String, b: Boolean)
//
//  val data: Codec[Data] =
//    (int4 *: bpchar *: bool).to[Data]
//
//  // SQL depends on the number of `Data` elements we wish to "insert"
//  def query(len: Int): Query[List[Data], Data] =
//    sql"VALUES ${data.values.list(len)}".query(data)
//
//  val examples: List[Data] =
//    List(
//      Data(10, "foo", true),
//      Data(11, "bar", true),
//      Data(12, "baz", false),
//    )
//
//  def run(args: List[String]): ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
//    given Console[Task] = Console.make[Task]
//    val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
//      for
//        config <- ZIO.service[AppConfig]
//        res    <- AppResources.make(config).toScopedZIO
//      yield res
//    )=
//    session.use { s =>
//      val q = query(examples.length)
//      s.prepare(q).flatMap { pq =>
//        for {
//          _  <- IO(println(q.sql))
//          ds <- pq.stream(examples, 64).compile.to(List)
//          _  <- ds.traverse(d => IO(println(d)))
//          _  <- IO(println(s"Are they the same? ${ds == examples}"))
//        } yield ExitCode.Success
//      }
//    }

//}
