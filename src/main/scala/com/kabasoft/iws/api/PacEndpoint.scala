package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.repository.PacRepository
import zio._
import zio.http.api.HttpCodec.literal
import zio.http.api.{EndpointSpec, Middleware, MiddlewareSpec, RouteCodec}
import zio.http.middleware.Auth
import zio.schema.DeriveSchema.gen

object PacEndpoint {

  private val allPacAPI = EndpointSpec.get[Unit](literal("pac")).out[List[PeriodicAccountBalance]]
  //private val streamPacAPI = EndpointSpec.get[Unit](literal("pac")).outStream[PeriodicAccountBalance]
  private val pacByIdAPI = EndpointSpec.get[String](literal("pac")/ RouteCodec.string("id") ).out[PeriodicAccountBalance]

  //private val streamPacEndpoint = streamPacAPI.implement { case () => PacRepository.list("1000").runCollect.map(_.toList)}
  private val allPacEndpoint = allPacAPI.implement { case () => PacRepository.all("1000")}
  private val pacByIdEndpoint = pacByIdAPI.implement { case (id: String) =>PacRepository.getBy(id,"1000")}


  val middlewareSpec: MiddlewareSpec[Auth.Credentials, Unit] = MiddlewareSpec.auth

  // just like api.handle
  val middleware: Middleware[Any, Auth.Credentials, Unit] =
    middlewareSpec.implementIncoming(_ => ZIO.unit)

  private val pacServiceSpec = (allPacAPI.toServiceSpec ++ pacByIdAPI.toServiceSpec)
    .middleware(middlewareSpec)

  val appPac = pacServiceSpec.toHttpApp(allPacEndpoint ++ pacByIdEndpoint, middleware)

}
