package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.{ financialsDerivedDecoder, financialsDerivedEncoder, financialsEncoder }
//import com.kabasoft.iws.repository.Schema.{derivedTransactionSchema, transactionDetailsSchema, transactionSchema, transactionSchema_}
import com.kabasoft.iws.service.FinancialsService
import com.kabasoft.iws.domain.{ AppError, DerivedTransaction }
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.model.{ Method, Status }
import zio.json._

object FinancialsHttpRoutes {

  val appFtr: Http[TransactionRepository with FinancialsService, AppError.RepositoryError, Request, Response] = Http.collectZIO[Request] {

    case Method.GET -> !! / "ftr" =>
      TransactionRepository
        .list("1000")
        .runCollect
        .map(ch => Response.json(ch.toJson))

    case Method.GET -> !! / "ftr" / "ftrmd" / modelid =>
      TransactionRepository
        .getByModelIdX(modelid.toInt, "1000")
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
    case Method.POST -> !! / "ftr" / "post" / id          =>
      FinancialsService.post(id.toLong, "1000").either.map {
        case Right(o) => Response.json(o.toJson)
        case Left(e)  => Response.text(e.getMessage() + "id:" + id)
      }

    /// ftr/ftrmd/114 getByModelId
    case req @ Method.POST -> !! / "ftr"                  =>
      (for {
        body <- req.body.asString
                  .flatMap(request =>
                    ZIO
                      .fromEither(request.fromJson[DerivedTransaction])
                      .mapError(e => new Throwable(e))
                  )
                  .mapError(e => AppError.DecodingError(e.getMessage))
                  .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _    <- TransactionRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_)  => Response.status(Status.BadRequest)
      }

  }
}
