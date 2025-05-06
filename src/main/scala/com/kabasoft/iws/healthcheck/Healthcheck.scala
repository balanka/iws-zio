package com.kabasoft.iws.healthcheck

import zio._
import zio.http._ 
import zio.http.codec.HttpCodec._

object Healthcheck

val expose: Routes[Any, Response] = Routes(
  Method.GET / "health"  ->
    handler { (_: Request) =>
      Response.text("Welcome  health!!!")
    },
)