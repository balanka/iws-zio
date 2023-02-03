package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, Supplier}
import com.kabasoft.iws.repository._
import zio._
import zio.http.api.HttpCodec.literal
import zio.http.api.{EndpointSpec, RouteCodec}
import zio.http._
import zio.http.model.{Method, Status}
import zio.json.DecoderOps
import zio.schema.DeriveSchema.gen

object SupplierEndpoint {

  //private val createAPI = EndpointSpec.post[Supplier](literal("sup")/RouteCodec.).out[Int]
  private val createEndpoint = Http.collectZIO[Request] {
    case req@Method.POST -> !! / "module" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[Supplier])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- SupplierRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
  private val allAPI = EndpointSpec.get[Unit](literal("sup")).out[List[Supplier]]
  private val byIdAPI = EndpointSpec.get[String](literal("sup")/ RouteCodec.string("id") ).out[Supplier]
  private val deleteAPI = EndpointSpec.get[String](literal("sup")/ RouteCodec.string("id") ).out[Int]

  private val allEndpoint = allAPI.implement (_ => SupplierRepository.all("1000"))
  private val byIdEndpoint = byIdAPI.implement (id =>SupplierRepository.getBy(id,"1000"))
  private val deleteEndpoint = deleteAPI.implement (id =>SupplierRepository.delete(id,"1000"))

  private val serviceSpec = (allAPI.toServiceSpec ++ byIdAPI.toServiceSpec++deleteAPI.toServiceSpec)


  val appSup: HttpApp[SupplierRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(allEndpoint ++ byIdEndpoint++deleteEndpoint)++createEndpoint
}
