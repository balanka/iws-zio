package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, User}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.model.{Method, Status}
import zio.json._

object UserHttpRoutes {

  val appUser = Http.collectZIO[Request] {

    case Method.GET -> !! / "user" =>
      UserRepository
        .list("1000")
        .runCollect
        .map(ch => Response.json(ch.toList.toJson))

    case Method.GET -> !! / "user" / name =>
      UserRepository.getByUserName(name, "1000").either.map {
        case Right(o) => Response.json(o.toJson)
        case Left(e) => Response.text(e.getMessage() + "userName" + name)
      }
    case req@Method.POST -> !! / "user" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[User])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        _ <- UserRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
}
