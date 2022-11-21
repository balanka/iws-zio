package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.service.AccountService
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.json._
import zio.http.model.{Method, Status}

object AccountHttpRoutes {

  val appAcc = Http.collectZIO[Request] {

      case Method.GET -> !! / "acc" =>
        AccountRepository
          .all("1000")
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! /"balance" / accountid / fromPeriod / toPeriod        =>
        AccountService.getBalance(accountid, fromPeriod.toInt, toPeriod.toInt, "1000").either.map {
          case Right(o) => Response.json((List(o)++o.subAccounts.toList).toJson)
          case Left(e)  => Response.text(e.getMessage + "accountid" + accountid + " fromPeriod: " + fromPeriod + " toPeriod:" + toPeriod)
        }
      case Method.POST -> !! / "close" / inStmtAccId / fromPeriod / toPeriod =>
        AccountService.closePeriod(fromPeriod.toInt, toPeriod.toInt, inStmtAccId, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage + "inStmtAccId" + inStmtAccId + " fromPeriod: " + fromPeriod + " toPeriod:" + toPeriod)
        }
      case Method.GET -> !! / "acc" / id                                     =>
        AccountRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage + "ID" + id)
        }

      case req @ Method.POST -> !! / "acc" =>
        (for {
          body <- req.body.asString
                    .flatMap(request =>
                      ZIO
                        .fromEither(request.fromJson[Account])
                        .mapError(e => new Throwable(e))
                    )
                    .mapError(e => AppError.DecodingError(e.getMessage))
                    .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          _    <- AccountRepository.create(body)
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_)  => Response.status(Status.BadRequest)
        }

    }
}
