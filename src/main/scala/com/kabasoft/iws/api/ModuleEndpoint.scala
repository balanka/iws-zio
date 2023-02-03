package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, Module}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{EndpointSpec, RouteCodec}
import zio.http.model.{Method, Status}
import zio.json._
import zio.schema.DeriveSchema.gen

object ModuleEndpoint {

  private val createEndpoint = Http.collectZIO[Request] {
    case req@Method.POST -> !! / "module" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[Module])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- ModuleRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
  private val allAPI = EndpointSpec.get[Unit](literal("module")).out[List[Module]]
  private val byIdAPI = EndpointSpec.get[String](literal("module")/ RouteCodec.string("id") ).out[Module]
  private val deleteAPI = EndpointSpec.get[String](literal("module")/ RouteCodec.string("id") ).out[Int]

  private val allEndpoint = allAPI.implement (_=> ModuleRepository.all("1000"))
  private val byIdEndpoint = byIdAPI.implement (id =>ModuleRepository.getBy(id,"1000"))
  private val deleteEndpoint = deleteAPI.implement(id =>ModuleRepository.delete(id,"1000"))
  private val serviceSpec = (allAPI.toServiceSpec ++ byIdAPI.toServiceSpec++deleteAPI.toServiceSpec)

  val appModule: HttpApp[ModuleRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(allEndpoint ++ byIdEndpoint++deleteEndpoint)++createEndpoint

}
