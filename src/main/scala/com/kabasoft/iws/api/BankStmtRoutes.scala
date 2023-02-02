package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, BankStatement}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.model.{Method, Status}
import zio.json._

object BankStmtRoutes {


  val appBankStmt = Http.collectZIO[Request] {
    case Method.GET -> !! / "bs" =>
      BankStatementRepository
        .list("1000")
        .runCollect
        .map(ch => Response.json(ch.toJson))

    case Method.GET -> !! / "bs" / id =>
      BankStatementRepository.getBy(id, "1000").either.map {
        case Right(bankStmt) => Response.json(bankStmt.toJson)
        case Left(e) => Response.text(e.getMessage() + "BID" + id)
      }
    case req@Method.POST -> !! / "bs" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[BankStatement])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- BankStatementRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }

}
