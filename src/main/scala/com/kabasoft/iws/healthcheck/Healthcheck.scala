package com.kabasoft.iws.healthcheck

import zio._
import zio.http._
import zio.http.{Http, HttpApp, Response}
import zio.http.model.{Method, Status}

object Healthcheck {
  val expose: HttpApp[Any, Throwable] = Http.collectZIO { case Method.GET -> !! / "health" =>
    ZIO.succeed(Response.status(Status.Ok))
  }
}
