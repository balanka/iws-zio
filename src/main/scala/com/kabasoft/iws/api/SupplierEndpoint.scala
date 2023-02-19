package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.supplierDecoder
import com.kabasoft.iws.repository.Schema.supplierschema
import com.kabasoft.iws.domain.{ AppError, Supplier }
import com.kabasoft.iws.repository._
import zio._
import zio.http.api.HttpCodec.literal
import zio.http.api.{ EndpointSpec, RouteCodec }
import zio.http._
import zio.http.model.{ Method, Status }
import zio.json.DecoderOps
object SupplierEndpoint {

  // private val createAPI = EndpointSpec.post[Supplier](literal("sup")/RouteCodec.).out[Int]
  val supCreateEndpoint = Http.collectZIO[Request] { case req @ Method.POST -> !! / "sup" =>
    (for {
      body <- req.body.asString
                .flatMap(request =>
                  ZIO
                    .fromEither(request.fromJson[List[Supplier]])
                    .mapError(e => new Throwable(e))
                )
                .mapError(e => AppError.DecodingError(e.getMessage()))
                .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
      _    <- SupplierRepository.create(body)
    } yield ()).either.map {
      case Right(_) => Response.status(Status.Created)
      case Left(_)  => Response.status(Status.BadRequest)
    }
  }
  val supAllAPI         = EndpointSpec.get[Unit](literal("sup")).out[List[Supplier]]
  val supByIdAPI        = EndpointSpec.get[String](literal("sup") / RouteCodec.string("id")).out[Supplier]
  private val deleteAPI = EndpointSpec.get[String](literal("sup") / RouteCodec.string("id")).out[Int]

  val supAllEndpoint         = supAllAPI.implement(_ => SupplierRepository.all("1000"))
  val supByIdEndpoint        = supByIdAPI.implement(id => SupplierRepository.getBy(id, "1000"))
  private val deleteEndpoint = deleteAPI.implement(id => SupplierRepository.delete(id, "1000"))

  private val serviceSpec = (supAllAPI.toServiceSpec ++ supByIdAPI.toServiceSpec ++ deleteAPI.toServiceSpec)

  val appSup: HttpApp[SupplierRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(supAllEndpoint ++ supByIdEndpoint ++ deleteEndpoint) ++ supCreateEndpoint
}
