package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.repository._
import zio._
import zio.http.api.HttpCodec.literal
import zio.http.{Body, Response}
import zio.http.api.{EndpointSpec, Middleware, MiddlewareSpec, RouteCodec}
import zio.http.middleware.Auth
import zio.http.model.{Headers, Status}

object BankEndpoint {

  //private val createBankAPI = EndpointSpec.post[Bank](literal("bank")/RouteCodec.).out[Int]
  private val allBankAPI = EndpointSpec.get[Unit](literal("bank")).out[List[Bank]]
  private val bankByIdAPI = EndpointSpec.get[String](literal("bank")/ RouteCodec.string("id") ).out[Bank]
  private val bankDeleteAPI = EndpointSpec.get[String](literal("bank")/ RouteCodec.string("id") ).out[Int]

  private val allBankEndpoint = allBankAPI.implement { case () => BankRepository.all("1000")}

  private val bankByIdEndpoint = bankByIdAPI.implement { case (id: String) =>BankRepository.getBy(id,"1000")}
  private val bankDeleteEndpoint = bankDeleteAPI.implement { case (id: String) =>BankRepository.delete(id,"1000")}

  val middlewareSpec: MiddlewareSpec[Auth.Credentials, Unit] = MiddlewareSpec.auth

  // just like api.handle
  val middleware: Middleware[Any, Auth.Credentials, Unit] =
    middlewareSpec.implementIncoming(_ => ZIO.unit)

  private val bankServiceSpec = (allBankAPI.toServiceSpec ++ bankByIdAPI.toServiceSpec++bankDeleteAPI.toServiceSpec)
    .middleware(middlewareSpec)

  val appBank: http.App[BankRepository] = bankServiceSpec.toHttpApp(allBankEndpoint ++ bankByIdEndpoint++bankDeleteEndpoint, middleware)
                   .mapError(e=>Response(Status.InternalServerError, Headers.empty, Body.fromString(e.getMessage)))
  //.withDefaultErrorResponse
}
