package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.vatDecoder
import com.kabasoft.iws.repository.Schema.vatSchema
import com.kabasoft.iws.domain.{AppError, Vat}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{EndpointSpec, RouteCodec}
import zio.http.model.{Method, Status}
import zio.json.DecoderOps

object VatEndpoint {

  //private val createAPI = EndpointSpec.post[Bank](literal("bank")/RouteCodec.).out[Int]
  val vatCreateEndpoint: Http[VatRepository, Nothing, Request, Response] = Http.collectZIO[Request] {
    case req@Method.POST -> !! / "vat" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[List[Vat]])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable Vat body ${e}"))
        _ <- VatRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
   val vatAllAPI = EndpointSpec.get[Unit](literal("vat")).out[List[Vat]]
   val vatByIdAPI = EndpointSpec.get[String](literal("vat")/ RouteCodec.string("id") ).out[Vat]
  private val deleteAPI = EndpointSpec.get[String](literal("vat")/ RouteCodec.string("id") ).out[Int]

   val vatAllEndpoint = vatAllAPI.implement(_ => VatRepository.all("1000"))
   val vatByIdEndpoint = vatByIdAPI.implement  (id =>VatRepository.getBy(id,"1000"))
  private val deleteEndpoint = deleteAPI.implement(id =>VatRepository.delete(id,"1000"))

  private val serviceSpec = (vatAllAPI.toServiceSpec ++ vatByIdAPI.toServiceSpec++deleteAPI.toServiceSpec)

  val appVat: HttpApp[VatRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(vatAllEndpoint ++ vatByIdEndpoint++deleteEndpoint)++vatCreateEndpoint

}
