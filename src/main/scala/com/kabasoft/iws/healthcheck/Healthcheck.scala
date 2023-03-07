package com.kabasoft.iws.healthcheck

import zio._
import zio.http.model.{Method, Status}
import zio.http._
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object Healthcheck {
  val expose: HttpApp[Any, Throwable] = Http.collectZIO { case Method.GET -> !! / "health" =>
    ZIO.succeed(Response.status(Status.Ok))
  }


  //def health2Endpoint =
   // Endpoint.get("health2" / string("greeting")).out[String]
  val health2API = Endpoint.get("health2"/string("greeting")).out[String]

  //val health2Endpoint = health2API.implement(_ => ZIO.succeed("Welcome health2!!!"))
  val health2Endpoint = health2API.implement {case (greeting: String) => ZIO.succeed(s"Welcome ${greeting} health2!!!")}

  val health2 = health2Endpoint//.toApp@@ bearerAuth(jwtDecode(_).isDefined)

}
