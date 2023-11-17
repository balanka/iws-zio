package com.kabasoft.iws.healthcheck

import zio._
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object Healthcheck {
  val expose = Endpoint.get("health").out[String].implement (_ => ZIO.succeed(s"Welcome  health!!!"))

}
