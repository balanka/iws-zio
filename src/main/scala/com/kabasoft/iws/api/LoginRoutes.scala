package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.{ loginRequestDecoder, userCodec}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common.DummyUser
import com.kabasoft.iws.repository._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.ZIO
import zio.http._
import zio.http.Header.Custom
import zio.http.{Method, Status}
import zio.json.{DecoderOps, EncoderOps}

import java.time.Clock

object LoginRoutes {

  // Secret Authentication key
  val SECRET_KEY = "secretKey"

  implicit val clock: Clock = Clock.systemUTC

  // Helper to encode the JWT token
  def jwtEncode(username: String, liveSpan: Long): String = {
    // val json = s"""{"user": "${username}"}"""
    val json  = s"""{$username}"""
    val claim = JwtClaim {
      json
    }.issuedNow.expiresIn(liveSpan)
    Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)
  }

  // Helper to decode the JWT token
  def jwtDecode(token: String): Option[JwtClaim] = {
    val result = Jwt.decode(token, SECRET_KEY, Seq(JwtAlgorithm.HS512)).toOption
    println(s"jwtDecoded  $result")
    result
  }


  def appLogin = Http.collectZIO[Request] {
    case req@Method.POST -> Root / "users" / "login" =>
      for {

        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[LoginRequest])
              .mapError(e => RepositoryError(e))
              .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          )
          // .either
          .flatMap (checkLogin)
      } yield body
  }

  //def loginAPI = Endpoint.post("users" / "login").in[LoginRequest].out[User].outError[RepositoryError](Status.InternalServerError)
  //val loginEndpoint = loginAPI.implement {
 //   loginRequest => checkLogin(loginRequest).mapError(e => RepositoryError(e.getMessage))
     // Response.json(user.toJson).addHeader(Header("authorization", token)).addHeader(Header("Origin", "localhost:3000"))
 // }
  private def checkLogin(loginRequest: LoginRequest): ZIO[UserRepository, RepositoryError, Response] = for {
    _ <- ZIO.logInfo(s"checkLogin >>>>>>")
    //r <- UserRepository.list(loginRequest.company).runCollect.map(_.toList)
    r <- UserRepository.list("1000").runCollect.map(_.toList)
    user = r.find(_.userName.equals(loginRequest.userName)).getOrElse(DummyUser)
    content   = jwtDecode(user.hash).toList.head.content.replace("{","").replace("}","")
    _ <- ZIO.logInfo(s"user >>>>>> ${user}")
    //_ <- ZIO.logInfo(s"pwd >>>>>> ${jwtEncode(loginRequest.password,1000000)}")
    _ <- ZIO.logInfo(s"password >>>>>> ${content} >>>>>   ${content.substring(1, content.length - 1)}")
  } yield {

    println(s"content >>>>>> $content")
    val pwd       = content//.substring(1, content.length - 1).replaceAll("\"", "")
    val pwdR      = loginRequest.password
    val usernameR = loginRequest.userName
    val username  = user.userName
    val check     = (usernameR == username) & (pwdR == pwd)
    if (check) {
      val json = s"""{"${loginRequest.password}"}"""
      val token = jwtEncode(json, 1000000)
      Response.json(user.toJson).addHeader(Custom("authorization", token))//.addHeader(Custom("Origin", "http://Mac-Studio.fritz.box:3000"))
        .addHeader(Custom("Origin", "http://localhost:3000"))
        .addHeader(Custom("Access-Control-Allow-Origin", "*"))
    } else {
      Response.text("Invalid  user name or password "
        + loginRequest.userName + "/"
        + loginRequest.password).withStatus(Status.Unauthorized)

    }
  }
}
