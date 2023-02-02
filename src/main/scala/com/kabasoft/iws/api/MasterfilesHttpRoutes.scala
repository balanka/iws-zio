package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, Bank, Costcenter, Module}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.model.{Method, Status}
import zio.json._

object MasterfilesHttpRoutes {

 val appBank = Http.collectZIO[Request] {

    case Method.GET -> !! / "bank" =>
      BankRepository.all("1000").either.map {
        case Right(res) => Response.json(res.toJson)
        case Left(e) => Response.text(e.getMessage())
      }

    case Method.GET -> !! / "bank" / id =>
      BankRepository.getBy(id, "1000").either.map {
        case Right(o) => Response.json(o.toJson)
        case Left(e) => Response.text(e.getMessage() + "ID" + id)
      }
    case req@Method.POST -> !! / "bank" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[Bank])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- BankRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
  val appModule = Http.collectZIO[Request] {

    case Method.GET -> !! / "module" =>
      ModuleRepository
        .list("1000")
        .runCollect
        .map(ch => Response.json(ch.toList.toJson))

    case Method.GET -> !! / "module" / id =>
      ModuleRepository.getBy(id, "1000").either.map {
        case Right(o) => Response.json(o.toJson)
        case Left(e) => Response.text(e.getMessage() + "ID" + id)
      }
    case req@Method.POST -> !! / "module" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[Module])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- ModuleRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
  val appCostcenter = Http.collectZIO[Request] {

    case Method.GET -> !! / "cc" =>
      CostcenterRepository
        .list("1000")
        .runCollect
        .map(ch => Response.json(ch.toList.toJson))

    case Method.GET -> !! / "cc" / id =>
      CostcenterRepository.getBy(id, "1000").either.map {
        case Right(o) => Response.json(o.toJson)
        case Left(e) => Response.text(e.getMessage() + "ID" + id)
      }
    case req@Method.POST -> !! / "cc" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[Costcenter])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- CostcenterRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }

}
