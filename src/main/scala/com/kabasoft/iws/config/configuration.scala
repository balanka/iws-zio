package com.kabasoft.iws.config

import zio._
import zio.config._
import zio.config.ConfigDescriptor._
import zio.config.typesafe.TypesafeConfigSource
import com.typesafe.config.ConfigFactory
import zio.config.magnolia._
import zio.sql.ConnectionPoolConfig

import java.util.Properties

final case class ServerConfig(port: Int)

object ServerConfig {

  private val serverConfigDescription = nested("server-config") {
    int("port").default(8090)
  }.to[ServerConfig]

  val layer = ZLayer(
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

  val dbConfigDescriptor = nested("db-config")(descriptor[DbConfig])

  val layer = ZLayer(
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
      } yield ConnectionPoolConfig(config.url, connProperties( config.user, config.password))
    )

  private def connProperties(user: String, password: String): Properties = {
    val props = new Properties
    props.setProperty("user", user)
    props.setProperty("password", password)
    props
  }
}
