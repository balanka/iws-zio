/*package com.kabasoft.iws.config

import com.typesafe.config.ConfigFactory
import zio.*
import zio.Config
import zio.config.*
//import zio.config.ConfigDescriptor.*
//import ConfigDescriptor.*
import zio.config.magnolia.*
import zio.config.typesafe.*
import zio.config.magnolia.deriveConfig
import java.util.Properties

final case class ServerConfig(host:String, port: Int)

object ServerConfig {

  private val serverConfigDescription = nested("server-config") {
   string("host").default("localhost").zip(int("port").default(8091))
  }.to[ServerConfig]

  val layer: ZLayer[Any, Throwable, ServerConfig] = ZLayer(
    ZIO
      .attempt(
        TypesafeConfigSource.fromTypesafeConfig(
          ZIO.attempt(ConfigFactory.defaultApplication())
        )
      )
      .map(source => serverConfigDescription from source)
      .flatMap(config => read(config))
      .orDie
  )
}
final case class AppConfig(
                            postgreSQL: AppConfig.PostgreSQLConfig,
                            httpServer: AppConfig.HttpServerConfig,
                            tradingConfig: AppConfig.TradingConfig
                          )

object AppConfig:
   final case class PostgreSQLConfig(
                                   host: String,
                                   port: Int,
                                   user: String,
                                   password: String, // @todo : need to change to Secret
                                   database: String,
                                   max: Int
                                 )

   final case class HttpServerConfig(host: String, port: Int)

   final case class TradingConfig(
                                maxAccountNoLength: Int,
                                minAccountNoLength: Int,
                                zeroBalanceAllowed: Boolean
                              )

final case class DbConfig(
  host: String,
  port: String,
  dbName: String,
  url: String,
  user: String,
  password: String,
  driver: String,
  connectThreadPoolSize: Int
)

object DbConfig {
  /**
   * Configuration information for the connection pool.
   *
   * @param url           The JDBC connection string.
   * @param properties    JDBC connection properties (username / password could go here).
   * @param poolSize      The size of the pool.
   * @param queueCapacity The capacity of the queue for connections. When this size is reached, back pressure will block attempts to add more.
   * @param retryPolicy   The retry policy to use when acquiring connections.
   */
  final case class ConnectionPoolConfig(
                                         url: String,
                                         properties: java.util.Properties,
                                         poolSize: Int = 10,
                                         queueCapacity: Int = 1000,
                                         autoCommit: Boolean = true,
                                         retryPolicy: Schedule[Any, Exception, Any] = Schedule.recurs(20) && Schedule.exponential(10.millis)
                                       )

  private val dbConfigDescriptor = nested("db-config")(descriptor[DbConfig])

  val layer: ZLayer[Any, Throwable, DbConfig]  = ZLayer(
    ZIO
      .attempt(
        TypesafeConfigSource.fromTypesafeConfig(
          ZIO.attempt(ConfigFactory.defaultApplication())
        )
      )
      .map(source => dbConfigDescriptor from source)
      .flatMap(config => read(config))
      .orDie
  )

  val connectionPoolConfig: ZLayer[DbConfig, Throwable, ConnectionPoolConfig] =
    ZLayer(
      for {
        config <- ZIO.service[DbConfig]
      } yield ConnectionPoolConfig(config.url, connProperties(config.user, config.password))
    )

  private def connProperties(user: String, password: String): Properties = {
    val props = new Properties
    props.setProperty("user", user)
    props.setProperty("password", password)
    props
  }

  final val Root = "tradex"
  private final val Descriptor = deriveConfig[AppConfig]

  val appConfig = ZLayer(TypesafeConfigProvider.fromResourcePath().nested(Root).load(Descriptor))
}

 */
