package com.kabasoft.iws

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import zio._
import zio.stream.ZStream
import zio.http._
import zio.http.codec.HttpCodec
import zio.http.endpoint.Endpoint
import zio.http.endpoint.EndpointMiddleware.None

import java.lang.System
import zio.http.Server.Config

object ServerSentEventEndpoint extends ZIOAppDefault {
  import HttpCodec._

  val stream: ZStream[Any, Nothing, ServerSentEvent] =
    ZStream.repeatWithSchedule(ServerSentEvent(ISO_LOCAL_TIME.format(LocalDateTime.now)), Schedule.spaced(1.second))

  val sseEndpoint: Endpoint[Unit, ZNothing, ZStream[Any, Nothing, ServerSentEvent], None] =
    Endpoint.get("sse").outStream[ServerSentEvent]

  private val sseRoute = sseEndpoint.implement(_=>ZIO.logInfo(s" http server started")*>ZIO.succeed(stream))

  val env = System.getenv()

  val hostName =if(env.keySet().contains("IWS_API_HOST")) env.get("IWS_API_HOST") else "0.0.0.0"
  val port = if(env.keySet().contains("IWS_API_PORT")) env.get("IWS_API_PORT").toInt else 8080
  println("hostName>>>"+hostName)
  println("hostport>>>"+port)
  private val serverLayer: ZLayer[Any, Throwable, Server] = {
    implicit val trace = Trace.empty
    //ZIO.logInfo(s"Starting http server") *>
    ZLayer.succeed(
      Config.default.binding(hostName, port)
    ) >>> Server.live
  }


  val app= sseRoute.toApp //toHttpApp

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    ZIO.logInfo(s"Starting http server") *>
    //Server.serve(app).provide(Server.default).exitCode
    Server.serve(app).provide(serverLayer)//.exitCode
  }

}