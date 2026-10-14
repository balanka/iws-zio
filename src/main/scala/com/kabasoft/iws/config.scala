package com.kabasoft.iws

import zio.config.*
import typesafe.*
import magnolia.*
import zio.{Config, ConfigProvider, ZLayer}

object config:
  final case class AppConfig(postgreSQL: AppConfig.PostgreSQLConfig)

  object AppConfig:
    final case class PostgreSQLConfig(
                                       host: String,
                                       port: Int,
                                       user: String,
                                       password: String, // @todo : need to change to Secret
                                       database: String,
                                       max: Int
                                     )


  final val Root = "tradex"

  private final val Descriptor = deriveConfig[AppConfig]

  /**
   * Config provider that works in both JVM and native-image:
   * - Prefers environment variables (reliable in native binaries / containers)
   * - Falls back to application.conf via the classpath (JVM dev)
   */
  private val provider: ConfigProvider =
    ConfigProvider
      .fromEnv()
      .nested(Root)
      .orElse(TypesafeConfigProvider.fromResourcePath().nested(Root))

  val appConfig: ZLayer[Any, Config.Error, AppConfig] =
    ZLayer(provider.load(Descriptor))