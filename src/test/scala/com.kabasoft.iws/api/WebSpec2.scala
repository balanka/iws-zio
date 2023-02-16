package com.kabasoft.iws.api

import com.kabasoft.iws.api.AccountEndpoint.appAcc
import com.kabasoft.iws.api.BankStmtEndpoint.appBankStmt
import com.kabasoft.iws.api.BankEndpoint.appBank
import com.kabasoft.iws.api.CostcenterEndpoint.appCC
import com.kabasoft.iws.api.CustomerEndpoint.appCust
import com.kabasoft.iws.api.ModuleEndpoint.appModule
import com.kabasoft.iws.api.SupplierEndpoint.appSup
import com.kabasoft.iws.api.UserEndpoint.appUser
import com.kabasoft.iws.api.VatEndpoint.appVat
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.AccountServiceImpl
import zio.sql.ConnectionPool
import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository.container.PostgresContainer
import zio.ZIO
import zio.http.Middleware._
import zio.test.Assertion.equalTo
import zio.http._
import zio.test._

object WebSpec2 extends ZIOSpecDefault with HttpAppTestExtensions { self =>

    def spec = suite("HttpMiddleware")(
      suite("debug")(
        test("health check") {
          val program = runApp(expose @@ debug, Request.get(url = URL(!! / "health")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /health 0ms\n")))
        },
        test("Account all") {
          val program = runApp(appAcc @@ debug, Request.get(url = URL(!! / "acc")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /acc 0ms\n")))
        },
        test("Account by id") {
          val program = runApp(appAcc @@ debug, Request.get(url = URL(!! / "acc" / "9900")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /acc/9900 0ms\n")))
        },
        test("Bank all") {
            val program = runApp(appBank @@ debug, Request.get(url = URL(!! / "bank")))*> TestConsole.output
             assertZIO(program)(equalTo(Vector("200 GET /bank 0ms\n")))
        },
        test("Bank by id") {
          val program = runApp(appBank @@ debug, Request.get(url = URL(!! / "bank"/"COLSDE33")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /bank/COLSDE33 0ms\n")))
        },
         test("Bank statement  all") {
          val program = runApp(appBankStmt @@ debug, Request.get(url = URL(!! / "bs")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /bs 0ms\n")))
        },

        test("Cost center all") {
          val program = runApp(appCC @@ debug, Request.get(url = URL(!! / "cc")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /cc 0ms\n")))
        },
        test("Cost center by id") {
          val program = runApp(appCC @@ debug, Request.get(url = URL(!! / "cc" / "300")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /cc/300 0ms\n")))
        },
         test("User all") {
            val program = runApp(appUser @@ debug, Request.get(url = URL(!! / "user")))*> TestConsole.output
            assertZIO(program)(equalTo(Vector("200 GET /user 0ms\n")))
          },
        test("User by userName") {
          val program = runApp(appUser @@ debug, Request.get(url = URL(!! / "user" / "jdegoes011"))) *> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /user/jdegoes011 0ms\n")))
        },
        test("Module all") {
          val program = runApp(appModule @@ debug, Request.get(url = URL(!! / "module")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /module 0ms\n")))
        },
        test("Module by id") {
          val program = runApp(appModule @@ debug, Request.get(url = URL(!! / "module" / "0000")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /module/0000 0ms\n")))
        },
        test("Customer all") {
          val program = runApp(appCust @@ debug, Request.get(url = URL(!! / "cust")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /cust 0ms\n")))
        },
        test("Customer by id") {
          val program = runApp(appCust @@ debug, Request.get(url = URL(!! / "cust" / "5222")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /cust/5222 0ms\n")))
        },
        test("Supplier all") {
          val program = runApp(appSup @@ debug, Request.get(url = URL(!! / "sup")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /sup 0ms\n")))
        },
        test("Supplier by id") {
          val program = runApp(appSup @@ debug, Request.get(url = URL(!! / "sup" / "70000")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /sup/70000 0ms\n")))
        },
        test("Vat all") {
          val program = runApp(appVat @@ debug, Request.get(url = URL(!! / "vat")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /vat 0ms\n")))
        },
        test("Vat by id") {
          val program = runApp(appVat @@ debug, Request.get(url = URL(!! / "vat" / "v101")))*> TestConsole.output
          assertZIO(program)(equalTo(Vector("200 GET /vat/v101 0ms\n")))
        },

    ).provideShared(ConnectionPool.live, PostgresContainer.connectionPoolConfigLayer, PostgresContainer.createContainer
        , CustomerRepositoryImpl.live, ModuleRepositoryImpl.live, CostcenterRepositoryImpl.live, BankStatementRepositoryImpl.live
        , BankRepositoryImpl.live, AccountServiceImpl.live, AccountRepositoryImpl.live, PacRepositoryImpl.live,
        SupplierRepositoryImpl.live, UserRepositoryImpl.live, VatRepositoryImpl.live)
  )

  private def runApp[R, E](app: HttpApp[R, E], request: Request) : ZIO[R, Option[E], Response] =
    app.runZIO (request).fork.flatMap(_.join)

}
