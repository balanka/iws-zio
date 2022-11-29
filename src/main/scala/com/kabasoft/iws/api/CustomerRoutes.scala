package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio.http._
import zio._
import zio.json._
import zio.http.model.{Method, Status}
object CustomerRoutes {


  val appCust = Http.collectZIO[Request] {
      case Method.GET -> !! / "cust" =>
        CustomerRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! / "cust" / id   =>
        CustomerRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case req @ Method.POST -> !! / "cust" =>
        (for {
          body <- req.body.asString
                    .flatMap(request =>
                      ZIO
                        .fromEither(request.fromJson[Customer])
                        .mapError(e => new Throwable(e))
                    )
                    .mapError(e => AppError.DecodingError(e.getMessage()))
                    .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          _    <- CustomerRepository.create(body)
        } yield ()).either.map {
          case Right(_) => Response.status(Status.Created)
          case Left(_)  => Response.status(Status.BadRequest)
        }
    }

  val appComp = Http.collectZIO[Request] {
    case Method.GET -> !! / "comp" =>
      CompanyRepository
        .list("1000")
        .runCollect
        .map(ch => Response.json(ch.toJson))

    case Method.GET -> !! / "comp" / id =>
      CompanyRepository.getBy(id).either.map {
        case Right(o) => Response.json(o.toJson)
        case Left(e) => Response.text(e.getMessage() + "ID" + id)
      }
    case req@Method.POST -> !! / "comp" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[Company])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- CompanyRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
}