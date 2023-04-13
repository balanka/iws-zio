package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.{loginRequestDecoder, userEncoder}
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

  def buildClaim(text: String, lifeTime: Long) = JwtClaim(text).issuedNow.expiresIn(lifeTime)

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

  val invalidRequest = LoginRequest("InvalidUsername", "InvalidPassword")

  def appLogin = Http.collectZIO[Request] { case req@Method.POST -> !! / "users" / "login" =>
    for {
      body <- req.body.asString
        .flatMap(request =>
          ZIO
            .fromEither(request.fromJson[LoginRequest])
            .mapError(e => RepositoryError(e))
            .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
        )
       // .either
        .flatMap { loginRequest =>
          val loginRequest_ = loginRequest//.getOrElse(invalidRequest)
          checkLogin(loginRequest_)
        }
    } yield body
  }

  //def loginAPI = Endpoint.post("users" / "login").in[LoginRequest].out[User].outError[RepositoryError](Status.InternalServerError)
  //val loginEndpoint = loginAPI.implement {
 //   loginRequest => checkLogin(loginRequest).mapError(e => RepositoryError(e.getMessage))
     // Response.json(user.toJson).addHeader(Header("authorization", token)).addHeader(Header("Origin", "localhost:3000"))
 // }
  private def checkLogin(loginRequest: LoginRequest): ZIO[UserRepository, RepositoryError, Response] = for {
    r <- UserRepository.list("1000").runCollect.map(_.toList)
  } yield {
    val useropt   = r.find(_.userName.equals(loginRequest.userName))
    val user      = useropt.getOrElse(DummyUser)
    println(s"user >>>>>> ${user}")
    println(s"password >>>>>> ${jwtEncode(loginRequest.password,1000000)}")
    val content   = jwtDecode(user.hash).toList.head.content
    println(s"content >>>>>> $content")
    val pwd       = content.substring(1, content.length - 1).replaceAll("\"", "")
    val pwdR      = loginRequest.password
    val usernameR = loginRequest.userName
    val username  = user.userName
    val check     = ((usernameR == username) & (pwdR == pwd))//& (pwdR.substring(1, pwdR.length - 1) == pwd))
    if (check) {
      val k = "wuduwali2x"
      val json = s"""{"${k}"}"""
      //val json2 = s"""{"${pwd}"}"""
      val encoded = user.hash // Jwt.encode(buildClaim(json, 1000000), SECRET_KEY, JwtAlgorithm.HS512)
      val token = jwtEncode(json, 1000000)
      //val token2 = jwtEncode(json2, 1000000)
      println(s"encodedencodedencodedencodedencoded  ${user}")
      println(s"encodedencodedencodedencodedencoded  ${encoded}")
      println(s"token  ${token}")
      //println(s"token2  ${token2}")
      //@@ bearerAuth(jwtDecode(_).isDefined)

      Response.json(user.toJson).addHeader(Custom("authorization", token)).addHeader(Custom("Origin", "http://localhost:3000"))
        .addHeader(Custom("Access-Control-Allow-Origin", "http://localhost:3000"))
    } else {
      // ZIO.logInfo(s"Invalid  user name or password  ${username}")*>
      println(s"encodedencodedencodedencodedencoded  ${content} ----${pwd}")
      Response.text("Invalid  user name or password " + loginRequest.userName + "/" + loginRequest.password).withStatus(Status.Unauthorized)
      //DummyUser
    }
  }
}
