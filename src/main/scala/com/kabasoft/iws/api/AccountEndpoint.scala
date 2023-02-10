package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.accountDecoder
import com.kabasoft.iws.repository.Schema.accountSchema
import com.kabasoft.iws.domain.{Account, AppError}
import com.kabasoft.iws.service.AccountService
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{EndpointSpec, RouteCodec}
import zio.http.model.{Method, Status}
import zio.json.DecoderOps

object AccountEndpoint {

  private val createEndpoint = Http.collectZIO[Request] {
    case req@Method.POST -> !! / "acc" =>
      (for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[Account])
              .mapError(e => new Throwable(e))
          )
          .mapError(e => AppError.DecodingError(e.getMessage()))
          .tapError(e => ZIO.logInfo(s"Unparseable Vat body ${e}"))
        _ <- AccountRepository.create(body)
      } yield ()).either.map {
        case Right(_) => Response.status(Status.Created)
        case Left(_) => Response.status(Status.BadRequest)
      }
  }
  val balanceAPI = EndpointSpec.get[(String, Int, Int)](literal("balance")
    / RouteCodec.string("accId") / RouteCodec.int("from") / RouteCodec.int("to")).out[List[Account]]
   val closePeriodAPI = EndpointSpec.get[(String, Int, Int)](literal("close")
    / RouteCodec.string("accId") / RouteCodec.int("from") / RouteCodec.int("to")).out[Int]
  private val allAPI = EndpointSpec.get[Unit](literal("acc")).out[List[Account]]
  private val byIdAPI = EndpointSpec.get[String](literal("acc")/ RouteCodec.string("id") ).out[Account]
  private val deleteAPI = EndpointSpec.get[String](literal("acc")/ RouteCodec.string("id") ).out[Int]

  val closePeriodEndpoint =
    closePeriodAPI.implement((in) => AccountService.closePeriod(in._2, in._3, in._1, "1000"))
  val balanceEndpoint =
    balanceAPI.implement((in) => AccountService.getBalance(in._1, in._2, in._3, "1000"))
  private val allEndpoint = allAPI.implement(_ => AccountRepository.all("1000"))
  private val byIdEndpoint = byIdAPI.implement  (id =>AccountRepository.getBy(id,"1000"))
  private val deleteEndpoint = deleteAPI.implement(id =>AccountRepository.delete(id,"1000"))

  private val serviceSpec = (allAPI.toServiceSpec ++ byIdAPI.toServiceSpec++deleteAPI.toServiceSpec
    ++balanceAPI.toServiceSpec++closePeriodAPI.toServiceSpec)

  val appAcc: HttpApp[AccountRepository with AccountService, AppError.RepositoryError] =
    serviceSpec.toHttpApp(allEndpoint ++ byIdEndpoint++balanceEndpoint++closePeriodEndpoint++deleteEndpoint)++createEndpoint

}
