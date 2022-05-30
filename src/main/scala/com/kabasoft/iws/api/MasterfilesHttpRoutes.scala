package com.kabasoft.iws.api

import zhttp.http._
import zio._
import zio.json._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
//import com.kabasoft.iws.service._
import Protocol._

object MasterfilesHttpRoutes {

  val app: HttpApp[BankRepository with BankStatementRepository, Throwable] =
    Http.collectZIO {

      case Method.GET -> !! / "bank" =>
        BankRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! / "bank" / id   =>
        BankRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case req @ Method.POST -> !! / "bank" =>
        (for {
          body <- req.bodyAsString
                    .flatMap(request =>
                      ZIO
                        .fromEither(request.fromJson[Bank])
                        .mapError(e => new Throwable(e))
                    )
                    .mapError(e => AppError.DecodingError(e.getMessage()))
                    .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          _    <- BankRepository.create(body)
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_)  => Response.status(Status.BadRequest)
        }

      case Method.GET -> !! / "bankStatement" =>
        BankStatementRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! / "bankStatement" / bid  =>
        BankStatementRepository.getBy(bid, "1000").either.map {
          case Right(bankStmt) => Response.json(bankStmt.toJson)
          case Left(e)         => Response.text(e.getMessage() + "BID" + bid)
        }
      case req @ Method.POST -> !! / "bankStatement" =>
        (for {
          body <- req.bodyAsString
                    .flatMap(request =>
                      ZIO
                        .fromEither(request.fromJson[BankStatement])
                        .mapError(e => new Throwable(e))
                    )
                    .mapError(e => AppError.DecodingError(e.getMessage()))
                    .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          _    <- BankStatementRepository.create(body)
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_)  => Response.status(Status.BadRequest)
        }
    }
}
