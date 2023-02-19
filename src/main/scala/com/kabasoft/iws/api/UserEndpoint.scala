package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.userDecoder
import com.kabasoft.iws.repository.Schema.userSchema
import com.kabasoft.iws.domain.{ AppError, User }
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{ EndpointSpec, RouteCodec }
import zio.http.model.{ Method, Status }
import zio.json.DecoderOps

object UserEndpoint {

  // private val createAPI = EndpointSpec.post[User](literal("user")/RouteCodec.).out[Int]
  val userCreateEndpoint = Http.collectZIO[Request] { case req @ Method.POST -> !! / "user" =>
    (for {
      body <- req.body.asString
                .flatMap(request =>
                  ZIO
                    .fromEither(request.fromJson[List[User]])
                    .mapError(e => new Throwable(e))
                )
                .mapError(e => AppError.DecodingError(e.getMessage()))
                .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
      _    <- UserRepository.create(body)
    } yield ()).either.map {
      case Right(_) => Response.status(Status.Created)
      case Left(_)  => Response.status(Status.BadRequest)
    }
  }
  val userAllAPI         = EndpointSpec.get[Unit](literal("user")).out[List[User]]
  val userByIdAPI        = EndpointSpec.get[Int](literal("user") / RouteCodec.int("id")).out[User]
  val userByUserNameAPI  = EndpointSpec.get[String](literal("user") / RouteCodec.string("userName")).out[User]
  private val deleteAPI  = EndpointSpec.get[Int](literal("user") / RouteCodec.int("id")).out[Int]

  val userAllEndpoint        = userAllAPI.implement(_ => UserRepository.all("1000"))
  val userByIdEndpoint       = userByIdAPI.implement(id => UserRepository.getById(id, "1000"))
  val userByUserNameEndpoint = userByUserNameAPI.implement(userName => UserRepository.getByUserName(userName, "1000"))
  private val deleteEndpoint = deleteAPI.implement(id => UserRepository.delete(id, "1000"))

  private val serviceSpec = (userAllAPI.toServiceSpec ++ userByIdAPI.toServiceSpec ++ deleteAPI.toServiceSpec ++ userByUserNameAPI.toServiceSpec)

  val appUser: HttpApp[UserRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp(userAllEndpoint ++ userByIdEndpoint ++ deleteEndpoint ++ userByUserNameEndpoint) ++ userCreateEndpoint
}
