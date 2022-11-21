package com.kabasoft.iws.api

import com.kabasoft.iws.api.AccountHttpRoutes.appAcc
import com.kabasoft.iws.api.MasterfilesHttpRoutes.{appBank, appBankStmt, appModule, appUser}
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.healthcheck.Healthcheck.expose
import com.kabasoft.iws.repository.{AccountRepositoryImpl, BankRepositoryImpl, BankStatementRepositoryImpl, ModuleRepositoryImpl, PacRepositoryImpl, UserRepositoryImpl}
import com.kabasoft.iws.service.AccountServiceImpl
import zio._
import zio.http.Middleware._
import zio.http._
import zio.sql.ConnectionPool
import zio.test.Assertion._
import zio.test._

import scala.language.postfixOps

object WebSpec2 extends ZIOSpecDefault with HttpAppTestExtensions { self =>


  def spec = suite("HttpMiddleware")(
    suite("debug")(
      test("health check") {
        val program = runApp(expose @@ debug, Request.get(url = URL(!! / "health")))*> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /health 0ms\n")))
      },
      test("Account all") {
        val program = runApp(appAcc @@ debug, Request.get(url = URL(!! / "acc")))
          .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer,
            AccountServiceImpl.live, AccountRepositoryImpl.live, PacRepositoryImpl.live) *> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /acc 0ms\n")))
      },
      test("Account by id") {
        val program = runApp(appAcc @@ debug, Request.get(url = URL(!! / "acc" / "id=9900")))
          .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer,
            AccountServiceImpl.live, AccountRepositoryImpl.live, PacRepositoryImpl.live) *> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /acc/id=9900 0ms\n")))
      },
      test("Bank all") {
          val program = runApp(appBank @@ debug, Request.get(url = URL(!! / "bank")))
            .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, BankRepositoryImpl.live) *> TestConsole.output
           assertZIO(program)(equalTo(Vector("200 GET /bank 0ms\n")))
      },
      test("Bank by id") {
        val program = runApp(appBank @@ debug, Request.get(url = URL(!! / "bank"/"id=COLSDE33")))
          .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, BankRepositoryImpl.live) *> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /bank/id=COLSDE33 0ms\n")))
      },
      test("Bank statement  all") {
        val program = runApp(appBankStmt @@ debug, Request.get(url = URL(!! / "bs")))
          .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, BankStatementRepositoryImpl.live) *> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /bs 0ms\n")))
      },
      test("User statement by userName") {
        val program = runApp(appUser @@ debug, Request.get(url = URL(!! / "user"/"name=myUserName")))
          .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, UserRepositoryImpl.live) *> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /user/name=myUserName 0ms\n")))
      },
      test("Module all") {
        val program = runApp(appModule @@ debug, Request.get(url = URL(!! / "module")))
          .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, ModuleRepositoryImpl.live) *> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /module 0ms\n")))
      },
      test("Module by id") {
        val program = runApp(appModule @@ debug, Request.get(url = URL(!! / "module" / "id=1000")))
          .provide(ConnectionPool.live, connectionPoolConfig, DbConfig.layer, ModuleRepositoryImpl.live) *> TestConsole.output
        assertZIO(program)(equalTo(Vector("200 GET /module/id=1000 0ms\n")))
      },

    )

  )

  private def runApp[R, E](app: HttpApp[R, E], request: Request): ZIO[R, Option[E], Response] =
    app { request }.fork.flatMap(_.join)

}
