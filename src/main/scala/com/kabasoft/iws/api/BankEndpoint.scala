package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.bankDecoder
import com.kabasoft.iws.repository.Schema.bankSchema
import com.kabasoft.iws.domain.{ AppError, Bank }
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{ EndpointSpec, RouteCodec }
import zio.http.model.{ Method, Status }
import zio.json.DecoderOps
object BankEndpoint {

  val bankCreateEndpoint = Http.collectZIO[Request] { case req @ Method.POST -> !! / "bank" =>
    (for {
      body <- req.body.asString
                .flatMap(request =>
                  ZIO
                    .fromEither(request.fromJson[List[Bank]])
                    .mapError(e => new Throwable(e))
                )
                .mapError(e => AppError.DecodingError(e.getMessage))
                .tapError(e => ZIO.logInfo(s"Unparseable Bank body ${e}"))
      _    <- BankRepository.create(body)
    } yield ()).either.map {
      case Right(_) => Response.status(Status.Created)
      case Left(_)  => Response.status(Status.BadRequest)
    }
  }

  val bankAllAPI        = EndpointSpec.get[Unit](literal("bank")).out[List[Bank]]
  val bankByIdAPI       = EndpointSpec.get[String](literal("bank") / RouteCodec.string("id")).out[Bank]
  private val deleteAPI = EndpointSpec.get[String](literal("bank") / RouteCodec.string("id")).out[Int]

  val bankAllEndpoint        = bankAllAPI.implement(_ => BankRepository.all("1000"))
  val bankByIdEndpoint       = bankByIdAPI.implement(id => BankRepository.getBy(id, "1000"))
  private val deleteEndpoint = deleteAPI.implement(id => BankRepository.delete(id, "1000"))

  // val middlewareSpec: MiddlewareSpec[Auth.Credentials, Unit] = MiddlewareSpec.auth

  // just like api.handle
  // val middleware: Middleware[Any, Auth.Credentials, Unit] =
  //   middlewareSpec.implementIncoming(_ => ZIO.unit)

  private val serviceSpec = (bankAllAPI.toServiceSpec ++ bankByIdAPI.toServiceSpec ++ deleteAPI.toServiceSpec)
  // .middleware(middlewareSpec)

  val appBank: HttpApp[BankRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(bankAllEndpoint ++ bankByIdEndpoint ++ deleteEndpoint) ++ bankCreateEndpoint

}
