package com.kabasoft.iws.api

import com.kabasoft.iws.api.BankEndpoint.{allBankAPI, bankByIdAPI, _}
import com.kabasoft.iws.config.DbConfig
import com.kabasoft.iws.config.DbConfig.connectionPoolConfig
import com.kabasoft.iws.repository.{BankRepository, BankRepositoryImpl}
import zio.http._
import zio.http.api.{EndpointExecutor, EndpointRegistry, RouteCodec}
import zio.http.netty.client.ConnectionPool
import zio.sql.{ConnectionPool => SConnectionPool}
import zio.test.{ZIOSpecDefault, assertTrue}
import zio.{Scope, ZIO, ZLayer}

import scala.language.implicitConversions

object BankIntegrationSpec extends ZIOSpecDefault {
  implicit def stringToIn(s: String): RouteCodec[Unit] = RouteCodec.literal(s)

  def makeExecutor(client: Client ) = {
    val registry = EndpointRegistry(
      URL.fromString("http://localhost:8080").getOrElse(???),
        allBankAPI.toServiceSpec ++bankByIdAPI.toServiceSpec,
    )

    EndpointExecutor(client, registry, ZIO.unit)
  }
  //final case class Invocation[Id, A, B](api: EndpointSpec[A, B], input: A)
  val executorLayer = ZLayer.fromFunction(makeExecutor _)
  type I = EndpointExecutor[Any,Any,BankEndpoint.allBankAPI.type with BankEndpoint.bankByIdAPI.type] with BankRepository with Server
  type O = EndpointExecutor[Any,Any,BankEndpoint.allBankAPI.type with BankEndpoint.bankByIdAPI.type]

  def executor(): ZIO[I, Nothing,O]
  = for {
    _ <- Server.install((allBankHandler ++ bankByIdHandler).toHttpApp)
    _ <- ZIO.debug("Installed server")
    executor <- ZIO.service[EndpointExecutor[Any, Any, allBankAPI.type with bankByIdAPI.type]]
  } yield executor

  def spec =
    suite("BankIntegrationSpec")(
        test("server and client bank integration") {
        for {
          exec   <- executor
          result   <- exec(allBankAPI())

        } yield { println("result: "+result ); assertTrue(true)}
      },
    ).provideSome[Scope](
      Server.live,
      ServerConfig.live,
      Client.live,
      ConnectionPool.disabled,
      executorLayer,
      ClientConfig.default,
      SConnectionPool.live, connectionPoolConfig, DbConfig.layer, BankRepositoryImpl.live,
    )
}
