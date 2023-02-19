package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.pacDecoder
import com.kabasoft.iws.repository.Schema.pacSchema
import com.kabasoft.iws.domain.{ AppError, PeriodicAccountBalance }
import com.kabasoft.iws.repository.PacRepository
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{ EndpointSpec, RouteCodec }
import zio.http.model.{ Method, Status }
import zio.json.DecoderOps

object PacEndpoint {

  private val createEndpoint  = Http.collectZIO[Request] { case req @ Method.POST -> !! / "pac" =>
    (for {
      body <- req.body.asString
                .flatMap(request =>
                  ZIO
                    .fromEither(request.fromJson[PeriodicAccountBalance])
                    .mapError(e => new Throwable(e))
                )
                .mapError(e => AppError.DecodingError(e.getMessage()))
                .tapError(e => ZIO.logInfo(s"Unparseable PeriodicAccountBalance body ${e}"))
      _    <- PacRepository.create(List(body))
    } yield ()).either.map {
      case Right(_) => Response.status(Status.Created)
      case Left(_)  => Response.status(Status.BadRequest)
    }
  }
  private val allPacAPI       = EndpointSpec.get[Unit](literal("pac")).out[List[PeriodicAccountBalance]]
  // private val streamPacAPI = EndpointSpec.get[Unit](literal("pac")).outStream[PeriodicAccountBalance]
  private val pacByIdAPI      = EndpointSpec.get[String](literal("pac") / RouteCodec.string("id")).out[PeriodicAccountBalance]
  private val allPacEndpoint  = allPacAPI.implement(_ => PacRepository.all("1000"))
  private val pacByIdEndpoint = pacByIdAPI.implement(id => PacRepository.getBy(id, "1000"))

  private val pacServiceSpec = (allPacAPI.toServiceSpec ++ pacByIdAPI.toServiceSpec)

  val appPac = pacServiceSpec.toHttpApp(allPacEndpoint ++ pacByIdEndpoint) ++ createEndpoint

}
