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
import zio.http.Middleware._
import zio.test.Assertion.equalTo
import zio.http._
import zio.test._

object WebSpec2 extends ZIOSpecDefault with HttpAppTestExtensions { self =>

    def spec = suite("API test")(
      suite("AOI integration test")(
        test("health check") {
          val program = runApp(expose @@ debug, Request.get(url = URL(!! / "health")))
          assertZIO(program)(equalTo(Vector("200 GET /health 0ms\n")))
        },
        test("Get all account all") {
          val program = runApp(appAcc.toApp @@ debug, Request.get(url = URL(!! / "acc"/"1000")))
          assertZIO(program)(equalTo(Vector("200 GET /acc/1000 0ms\n")))
        },
        test("Get an account by its id") {
          val program = runApp(appAcc.toApp @@ debug, Request.get(url = URL(!! / "acc" / "9900")))
          assertZIO(program)(equalTo(Vector("200 GET /acc/9900 0ms\n")))
        },
        test("Get all Bank all") {
            val program = runApp(appBank.toApp @@ debug, Request.get(url = URL(!! / "bank"/"1000")))
             assertZIO(program)(equalTo(Vector("200 GET /bank/1000 0ms\n")))
        },
        test("Get a  bank by id") {
          val program = runApp(appBank.toApp @@ debug, Request.get(url = URL(!! / "bank"/"COLSDE33")))
          assertZIO(program)(equalTo(Vector("200 GET /bank/COLSDE33 0ms\n")))
        },
         test("Get all Bank statement") {
          val program = runApp(appBankStmt.toApp @@ debug, Request.get(url = URL(!! / "bs"/"1000")))
          assertZIO(program)(equalTo(Vector("200 GET /bs/1000 0ms\n")))
        },

        test("Get all cost center ") {
          val program = runApp(appCC.toApp @@ debug, Request.get(url = URL(!! / "cc"/"1000")))
          assertZIO(program)(equalTo(Vector("200 GET /cc/1000 0ms\n")))
        },
        test("Get a  cost center by id") {
          val program = runApp(appCC.toApp @@ debug, Request.get(url = URL(!! / "cc" / "300")))
          assertZIO(program)(equalTo(Vector("200 GET /cc/300 0ms\n")))
        },
         test("Get all users ") {
            val program = runApp(appUser.toApp @@ debug, Request.get(url = URL(!! / "user"/"1000")))
            assertZIO(program)(equalTo(Vector("200 GET /user/1000 0ms\n")))
          },
        test("Get a  user by userName") {
          val program = runApp(appUser.toApp @@ debug, Request.get(url = URL(!! / "user" / "jdegoes011")))
          assertZIO(program)(equalTo(Vector("200 GET /user/jdegoes011 0ms\n")))
        },
        test("Get all module") {
          val program = runApp(appModule.toApp @@ debug, Request.get(url = URL(!! / "module"/"1000")))
          assertZIO(program)(equalTo(Vector("200 GET /module/1000 0ms\n")))
        },
        test("Get a  module by id") {
          val program = runApp(appModule.toApp @@ debug, Request.get(url = URL(!! / "module" / "0000")))
          assertZIO(program)(equalTo(Vector("200 GET /module/0000 0ms\n")))
        },
        test("Get all customer ") {
          val program = runApp(appCust.toApp @@ debug, Request.get(url = URL(!! / "cust"/"1000")))
          assertZIO(program)(equalTo(Vector("200 GET /cust/1000 0ms\n")))
        },
        test("Get a customer by id") {
          val program = runApp(appCust.toApp @@ debug, Request.get(url = URL(!! / "cust" / "5222")))
          assertZIO(program)(equalTo(Vector("200 GET /cust/5222 0ms\n")))
        },
        test("Get all supplier") {
          val program = runApp(appSup.toApp @@ debug, Request.get(url = URL(!! / "sup"/"1000")))
          assertZIO(program)(equalTo(Vector("200 GET /sup/1000 0ms\n")))
        },
        test("Get a  supplier by id") {
          val program = runApp(appSup.toApp @@ debug, Request.get(url = URL(!! / "sup" / "70000")))
          assertZIO(program)(equalTo(Vector("200 GET /sup/70000 0ms\n")))
        },
        test("Get all vat") {
          val program = runApp(appVat.toApp @@ debug, Request.get(url = URL(!! / "vat"/"1000")))
          assertZIO(program)(equalTo(Vector("200 GET /vat/1000 0ms\n")))
        },
        test("Get a Vat by id") {
          val program = runApp(appVat.toApp @@ debug, Request.get(url = URL(!! / "vat" / "v101")))
          assertZIO(program)(equalTo(Vector("200 GET /vat/v101 0ms\n")))
        },

    ).provideShared(ConnectionPool.live, PostgresContainer.connectionPoolConfigLayer, PostgresContainer.createContainer
        , CustomerRepositoryImpl.live, ModuleRepositoryImpl.live, CostcenterRepositoryImpl.live
        , BankStatementRepositoryImpl.live, BankRepositoryImpl.live, AccountServiceImpl.live
        , AccountRepositoryImpl.live, PacRepositoryImpl.live, SupplierRepositoryImpl.live
        , UserRepositoryImpl.live, VatRepositoryImpl.live)
  )

  private def runApp[R, E](app: HttpApp[R, E], request: Request)=
    app.runZIO (request).fork.flatMap(_.join)*> TestConsole.output

}
