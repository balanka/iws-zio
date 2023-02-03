package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, BankStatement}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{EndpointSpec, RouteCodec}
import zio.http.model.{Method, Status}
import zio.json.DecoderOps

object BankStmtEndpoint {

  //private val createAPI = EndpointSpec.post[Bank](literal("bank")/RouteCodec.).out[Int]
  private val createEndpoint = Http.collectZIO[Request] {
    case req@Method.POST -> !! / "bs" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[BankStatement])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable Bank body ${e}"))
        _ <- BankStatementRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
  private val allAPI = EndpointSpec.get[Unit](literal("bs")).out[List[BankStatement]]
  private val byIdAPI = EndpointSpec.get[String](literal("bs")/ RouteCodec.string("id") ).out[BankStatement]
  private val deleteAPI = EndpointSpec.get[String](literal("bs")/ RouteCodec.string("id") ).out[Int]

  private val allEndpoint = allAPI.implement ( _=> BankStatementRepository.all("1000"))
  private val byIdEndpoint = byIdAPI.implement (id =>BankStatementRepository.getBy(id,"1000"))
  private val deleteEndpoint = deleteAPI.implement (id =>BankStatementRepository.delete(id,"1000"))

  private val serviceSpec = (allAPI.toServiceSpec ++ byIdAPI.toServiceSpec++deleteAPI.toServiceSpec)

  val appBankStmt: HttpApp[BankStatementRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(allEndpoint ++ byIdEndpoint++deleteEndpoint)++createEndpoint

}
