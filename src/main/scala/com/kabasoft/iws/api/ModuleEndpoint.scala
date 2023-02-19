package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.moduleDecoder
import com.kabasoft.iws.repository.Schema.moduleSchema
import com.kabasoft.iws.domain.{ AppError, Module }
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{ EndpointSpec, RouteCodec }
import zio.http.model.{ Method, Status }
import zio.json.DecoderOps

object ModuleEndpoint {

  val moduleCreateEndpoint = Http.collectZIO[Request] { case req @ Method.POST -> !! / "module" =>
    (for {
      body <- req.body.asString
                .flatMap(request =>
                  ZIO
                    .fromEither(request.fromJson[List[Module]])
                    .mapError(e => new Throwable(e))
                )
                .mapError(e => AppError.DecodingError(e.getMessage))
                .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
      _    <- ModuleRepository.create(body)
    } yield ()).either.map {
      case Right(_) => Response.status(Status.Created)
      case Left(_)  => Response.status(Status.BadRequest)
    }
  }
  val moduleAllAPI         = EndpointSpec.get[Unit](literal("module")).out[List[Module]]
  val moduleByIdAPI        = EndpointSpec.get[String](literal("module") / RouteCodec.string("id")).out[Module]
  private val deleteAPI    = EndpointSpec.get[String](literal("module") / RouteCodec.string("id")).out[Int]

  val moduleAllEndpoint      = moduleAllAPI.implement(_ => ModuleRepository.all("1000"))
  val moduleByIdEndpoint     = moduleByIdAPI.implement(id => ModuleRepository.getBy(id, "1000"))
  private val deleteEndpoint = deleteAPI.implement(id => ModuleRepository.delete(id, "1000"))
  private val serviceSpec    = (moduleAllAPI.toServiceSpec ++ moduleByIdAPI.toServiceSpec ++ deleteAPI.toServiceSpec)

  val appModule: HttpApp[ModuleRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(moduleAllEndpoint ++ moduleByIdEndpoint ++ deleteEndpoint) ++ moduleCreateEndpoint

}
