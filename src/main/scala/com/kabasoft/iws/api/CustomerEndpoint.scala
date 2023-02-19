package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.customerDecoder
import com.kabasoft.iws.repository.Schema.customerSchema
import com.kabasoft.iws.domain.{ AppError, Customer }
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{ EndpointSpec, RouteCodec }
import zio.http.model.{ Method, Status }
import zio.json.DecoderOps
import zio.schema.DeriveSchema.gen

object CustomerEndpoint {

  val custCreateEndpoint = Http.collectZIO[Request] { case req @ Method.POST -> !! / "cust" =>
    (for {
      body <- req.body.asString
                .flatMap(request =>
                  ZIO
                    .fromEither(request.fromJson[List[Customer]])
                    .mapError(e => new Throwable(e))
                )
                .mapError(e => AppError.DecodingError(e.getMessage))
                .tapError(e => ZIO.logInfo(s"Unparseable customer body ${e}"))
      _    <- CustomerRepository.create(body)
    } yield ()).either.map {
      case Right(_) => Response.status(Status.Created)
      case Left(_)  => Response.status(Status.BadRequest)
    }
  }
  val custAllAPI         = EndpointSpec.get[Unit](literal("cust")).out[List[Customer]]
  val custByIdAPI        = EndpointSpec.get[String](literal("cust") / RouteCodec.string("id")).out[Customer]
  private val deleteAPI  = EndpointSpec.get[String](literal("cust") / RouteCodec.string("id")).out[Int]

  val custAllEndpoint        = custAllAPI.implement { case () => CustomerRepository.all("1000") }
  val custByIdEndpoint       = custByIdAPI.implement(id => CustomerRepository.getBy(id, "1000"))
  private val deleteEndpoint = deleteAPI.implement(id => CustomerRepository.delete(id, "1000"))

  private val serviceSpec = (custAllAPI.toServiceSpec ++ custByIdAPI.toServiceSpec ++ deleteAPI.toServiceSpec)

  val appCust: HttpApp[CustomerRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(custAllEndpoint ++ custByIdEndpoint ++ deleteEndpoint) ++ custCreateEndpoint
}
