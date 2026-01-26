package com.kabasoft.iws.resources

import cats.effect.kernel.{Resource, Temporal}
import cats.effect.std.Console
import fs2.io.net.Network
import skunk.util.Typer
import skunk.{Session, SessionPool}
import zio.Task
import com.kabasoft.iws.config.AppConfig
import com.kabasoft.iws.config.AppConfig.PostgreSQLConfig


sealed abstract class AppResources private (val postgres: Resource[Task, Session[Task]])

object AppResources:
  def make(
      cfg: AppConfig
  )(using Temporal[Task], natchez.Trace[Task], Network[Task], Console[Task]): Resource[Task, AppResources] = {
    
    def mkPostgreSqlResource(c: PostgreSQLConfig): SessionPool[Task] =
      Session
        .pooled[Task](
          host = c.host,
          port = c.port,
          user = c.user,
          password = Some(c.password),
          database = c.database,
          max = c.max,
          strategy = Typer.Strategy.SearchPath
        )
    mkPostgreSqlResource(cfg.postgreSQL).map(r => new AppResources(r) {})
  }

