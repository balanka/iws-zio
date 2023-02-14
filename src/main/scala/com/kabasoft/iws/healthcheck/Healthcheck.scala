package com.kabasoft.iws.healthcheck

import zio._
import zio.http.model.{Method, Status}
import zio.http._

object Healthcheck {
  val expose: HttpApp[Any, Throwable] = Http.collectZIO { case Method.GET -> !! / "health" =>
    ZIO.succeed(Response.status(Status.Ok))
  }
}
