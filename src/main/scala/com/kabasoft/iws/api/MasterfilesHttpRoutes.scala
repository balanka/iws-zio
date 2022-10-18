package com.kabasoft.iws.api

import zhttp.http._
import zhttp.http.middleware.HttpMiddleware
import zio._
import zio.json._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
//import com.kabasoft.iws.service._
import com.kabasoft.iws.api.Protocol._
import java.io.IOException


object MasterfilesHttpRoutes {

  // catches errors and stops the default render of stack trace

  val errorMiddleware = new HttpMiddleware[Any, Throwable] {
    override def apply[R1 <: Any, E1 >: Throwable](
      http: HttpApp[R1, E1]
    ) =
      http.catchAll { ex =>
        val zio: ZIO[Any, IOException, Response] = for {
          _ <- ZIO.logError(ex.toString)
        } yield Response.status(Status.InternalServerError)
        Http.responseZIO(zio)
      }
  }

  val app: HttpApp[BankRepository with BankStatementRepository with ModuleRepository , Throwable] =
    Http.collectZIO {

      case Method.GET -> !! / "bank" =>
        BankRepository.all("1000").either.map {
          case Right(o) => { println("ZZOO"+o); val r=o.toJson;println("ZZ"+r);Response.json(r)}
          case Left(e)  => Response.text(e.getMessage() )
        }
          //.runCollect
          //.map(ch => {println("ZZ"+ch);Response.json(ch.toList.toJson)})

      case Method.GET -> !! / "bank" / id   =>
        BankRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case req @ Method.POST -> !! / "bank" =>
        (for {
          body <- req.body.asString
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
      case Method.GET -> !! / "module" =>
        ModuleRepository
          .list("1000")
          .runCollect
          //.map(ch =>Response.json(ch.toList.toJson))
          .map(ch => {println("ZZ"+ch);Response.json(ch.toList.toJson)})

      case Method.GET -> !! / "module" / id   =>
        ModuleRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case req @ Method.POST -> !! / "module" =>
        (for {
          body <- req.body.asString
            .flatMap(request =>
              ZIO
                .fromEither(request.fromJson[Module])
                .mapError(e => new Throwable(e))
            )
            .mapError(e => AppError.DecodingError(e.getMessage()))
            .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          _    <- ModuleRepository.create(body)
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
          body <- req.body.asString
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
