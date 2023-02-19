package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.companyDecoder
import com.kabasoft.iws.repository.Schema.companySchema
import com.kabasoft.iws.domain.{ AppError, Company }
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{ EndpointSpec, RouteCodec }
import zio.http.model.{ Method, Status }
import zio.json.DecoderOps
import zio.schema.DeriveSchema.gen

object CompanyEndpoint {

  private val createEndpoint = Http.collectZIO[Request] { case req @ Method.POST -> !! / "comp" =>
    (for {
      body <- req.body.asString
                .flatMap(request =>
                  ZIO
                    .fromEither(request.fromJson[Company])
                    .mapError(e => new Throwable(e))
                )
                .mapError(e => AppError.DecodingError(e.getMessage()))
                .tapError(e => ZIO.logInfo(s"Unparseable company body ${e}"))
      _    <- CompanyRepository.create(body)
    } yield ()).either.map {
      case Right(_) => Response.status(Status.Created)
      case Left(_)  => Response.status(Status.BadRequest)
    }
  }
  private val allAPI         = EndpointSpec.get[Unit](literal("comp")).out[List[Company]]
  private val byIdAPI        = EndpointSpec.get[String](literal("comp") / RouteCodec.string("id")).out[Company]
  private val deleteAPI      = EndpointSpec.get[String](literal("comp") / RouteCodec.string("id")).out[Int]

  private val allEndpoint    = allAPI.implement(_ => CompanyRepository.all)
  private val byIdEndpoint   = byIdAPI.implement(id => CompanyRepository.getBy(id))
  private val deleteEndpoint = deleteAPI.implement(id => CompanyRepository.delete(id))

  private val serviceSpec = (allAPI.toServiceSpec ++ byIdAPI.toServiceSpec ++ deleteAPI.toServiceSpec)

  val appComp: HttpApp[CompanyRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(allEndpoint ++ byIdEndpoint ++ deleteEndpoint) ++ createEndpoint
}
