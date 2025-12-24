package com.kabasoft.iws

import zio.config.*
import typesafe.*
import magnolia.*
import zio.{Config, ZLayer}

object config:
  final case class AppConfig(postgreSQL: AppConfig.PostgreSQLConfig
//                              , httpServer: AppConfig.HttpServerConfig,
//                              , tradingConfig: AppConfig.TradingConfig
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
//    final case class HttpServerConfig(host: String, port: Int)
//
//    final case class TradingConfig(
//                                    maxAccountNoLength: Int,
//                                    minAccountNoLength: Int,
//                                    zeroBalanceAllowed: Boolean
//                                  )

  final val Root = "tradex"

  private final val Descriptor = deriveConfig[AppConfig]

  val appConfig: ZLayer[Any, Config.Error, AppConfig] = ZLayer(TypesafeConfigProvider.fromResourcePath().nested(Root).load(Descriptor))