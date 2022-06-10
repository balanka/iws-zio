package com.kabasoft.iws

import zhttp.service.server.ServerChannelFactory
import zhttp.service.{ EventLoopGroup, Server }
import zio._
import zio.config._
import zio.sql.ConnectionPool
import com.kabasoft.iws.api._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service._
import com.kabasoft.iws.healthcheck.Healthcheck
import com.kabasoft.iws.config.{ DbConfig, ServerConfig }

object Main extends ZIOAppDefault {

  override def run =
    getConfig[ServerConfig]
      .map(config =>
        Server.port(config.port) ++
          Server.app(
            AccountHttpRoutes.app++FinancialsHttpRoutes.app
              ++HttpRoutes.app
              ++ MasterfilesHttpRoutes.app
              ++ PacHttpRoutes.app ++ VatHttpRoutes.app ++ Healthcheck.expose
          )
      )
      .flatMap(_.start)
      .provide(
        ServerConfig.layer,
        ServerChannelFactory.auto,
        EventLoopGroup.auto(),
        AccountRepositoryImpl.live,
        OrderRepositoryImpl.live,
        CustomerRepositoryImpl.live,
        BankRepositoryImpl.live,
        BankStatementRepositoryImpl.live,
        TransactionRepositoryImpl.live,
        PacRepositoryImpl.live,
        VatRepositoryImpl.live,
        QueryServiceImpl.live,
        JournalRepositoryImpl.live,
        FinancialsServiceImpl.live,
        DbConfig.layer,
        ConnectionPool.live,
        DbConfig.connectionPoolConfig
      )
}
