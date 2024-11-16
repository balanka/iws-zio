package com.kabasoft.iws.repository.container

import cats.effect.std.Console
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.kabasoft.iws.config.AppConfig
import com.kabasoft.iws.resources.AppResources
import org.testcontainers.utility.DockerImageName
import natchez.Trace.Implicits.noop
import zio.interop.catz.*
import zio.*

import java.time.Clock
//import zio.sql.ConnectionPoolConfig
import java.util.Properties

object PostgresContainer {
  given clock: Clock = Clock.systemUTC
  given Console[Task] = Console.make[Task]
  val appResourcesL: ZLayer[AppConfig, Throwable, AppResources] = ZLayer.scoped(
    for
      config <- ZIO.service[AppConfig]
      res <- AppResources.make(config).toScopedZIO
    yield res
  )
  val createContainer : ZLayer[Any, Throwable, PostgreSQLContainer] = 
    ZLayer.scoped {
      ZIO.acquireRelease {
        ZIO.attemptBlocking {
          val c = new PostgreSQLContainer(
            dockerImageNameOverride = Option("postgres:latest").map(DockerImageName.parse)
          ).configure { a =>
            a.withInitScript("init.sql")
            ()
          }
          c.start()
          c
        }
      } { container =>
        ZIO.attemptBlocking(container.stop()).orDie
      }
    }
//
//  val connectionPoolConfigLayer: ZLayer[PostgreSQLContainer, Throwable, ConnectionPoolConfig] = {
//    def connProperties(user: String, password: String): Properties = {
//      val props = new Properties
//      props.setProperty("user", user)
//      props.setProperty("password", password)
//      props
//    }
//
//    ZLayer((for {
//        c <- ZIO.service[PostgreSQLContainer]
//        container = c.container
//      } yield (ConnectionPoolConfig(
//        container.getJdbcUrl,
//        connProperties(container.getUsername, container.getPassword),
//      ))))
//  }
}