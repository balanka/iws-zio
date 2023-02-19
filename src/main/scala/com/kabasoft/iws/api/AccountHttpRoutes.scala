package com.kabasoft.iws.api

object AccountHttpRoutes {

  /*  val appAcc: Http[AccountRepository with AccountService, AppError.RepositoryError, Request, Response] = Http.collectZIO[Request] {

      case Method.GET -> !! / "acc1" =>
        AccountRepository
          .all("1000")
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! /"balance" / accountid / fromPeriod / toPeriod        =>
        AccountService.getBalance(accountid, fromPeriod.toInt, toPeriod.toInt, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage + "accountid" + accountid + " fromPeriod: " + fromPeriod + " toPeriod:" + toPeriod)
        }
      case Method.GET -> !! / "close" / inStmtAccId / fromPeriod / toPeriod =>
        AccountService.closePeriod(fromPeriod.toInt, toPeriod.toInt, inStmtAccId, "1000").either.map {
          case Right(o) => Response.json({println("close>>>>>>>>>>"+o); o.toJson})
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

    }*/
}
