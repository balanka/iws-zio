package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, Supplier}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.model.{Method, Status}
import zio.json._
object SupplierRoutes {

  val appSup: Http[SupplierRepository, RepositoryError, Request, Response] = Http.collectZIO[Request] {

      case Method.GET -> !! / "sup" =>
        SupplierRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! / "sup" / id   =>
        SupplierRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case req @ Method.POST -> !! / "sup" =>
        (for {
          body <- req.body.asString
                    .flatMap(request =>
                      ZIO
                        .fromEither(request.fromJson[Supplier])
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
}
