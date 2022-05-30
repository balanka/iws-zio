package com.kabasoft.iws.api

import zhttp.http._
import zio._
import zio.json._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import Protocol._

object PacHttpRoutes {

  val app: HttpApp[PacRepository, Throwable] =
    Http.collectZIO {

      case Method.GET -> !! / "pac" =>
        PacRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! / "pac" / id   =>
        PacRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case req @ Method.POST -> !! / "pac" =>
        (for {
          body <- req.bodyAsString
                    .flatMap(request =>
                      ZIO
                        .fromEither(request.fromJson[PeriodicAccountBalance])
                        .mapError(e => new Throwable(e))
                    )
                    .mapError(e => AppError.DecodingError(e.getMessage()))
                    .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          _    <- PacRepository.create(body)
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_)  => Response.status(Status.BadRequest)
        }

    }
}
