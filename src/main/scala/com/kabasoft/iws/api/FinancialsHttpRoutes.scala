package com.kabasoft.iws.api

import zhttp.http._
import zio._
import zio.json._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.FinancialsService
import Protocol._

object FinancialsHttpRoutes {

  val app: HttpApp[TransactionRepository with FinancialsService, Throwable] =
    Http.collectZIO {

      case Method.GET -> !! / "ftr" =>
        TransactionRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! / "ftr" / id                    =>
        TransactionRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case Method.GET -> !! / "ftr" / fromPeriod / toPeriod =>
        FinancialsService.postTransaction4Period(fromPeriod.toInt, toPeriod.toInt, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "fromPeriod:" + fromPeriod + "toPeriod:" + toPeriod)
        }
      case Method.POST -> !! / "ftr" /"post"/ id                   =>
        FinancialsService.post(id.toLong, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "id:" + id)
        }
      case req @ Method.POST -> !! / "ftr"                  =>
        (for {
          body <- req.bodyAsString
                    .flatMap(request =>
                      ZIO
                        .fromEither(request.fromJson[DerivedTransaction])
                        .mapError(e => new Throwable(e))
                    )
                    .mapError(e => AppError.DecodingError(e.getMessage()))
                    .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          _    <- TransactionRepository.create(body)
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_)  => Response.status(Status.BadRequest)
        }

    }
}
