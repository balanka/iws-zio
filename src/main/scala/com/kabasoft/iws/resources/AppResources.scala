package com.kabasoft.iws.resources

//import cats.effect._
import cats.effect.kernel.{Resource, Temporal}
import cats.effect.std.Console
//import cats.syntax.all._
import fs2.io.net.Network
//import skunk.codec.text._
//import skunk.implicits._
import skunk.util.Typer
import skunk.{Session, SessionPool}
import zio.Task
//import natchez.Trace.Implicits.noop // needed for skunk
import com.kabasoft.iws.config.AppConfig
import com.kabasoft.iws.config.AppConfig.PostgreSQLConfig
//import Tracer.Implicits.noop
//import org.typelevel.otel4s.trace.Tracer.Implicits.noop
//import zio.interop.catz.asyncInstance

sealed abstract class AppResources private (
    val postgres: Resource[Task, Session[Task]]
)

object AppResources:
  def make(
      cfg: AppConfig
  )(using Temporal[Task], natchez.Trace[Task], Network[Task], Console[Task]): Resource[Task, AppResources] = {
    //def checkPostgresConnection(postgres: Resource[Task, Session[Task]]): Task[Unit] =
      //postgres.use: session =>
        //session.unique(sql"select version();".query(text)).flatMap: v =>
          //ZIO.logInfo(s"Connected to Postgres $v")
    
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
        //.evalTap(checkPostgresConnection)

    mkPostgreSqlResource(cfg.postgreSQL).map(r => new AppResources(r) {})
  }

