package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.ZIO
import zio.http._
import zio.json._
import zio.http.model.{Header, Method, Status}
import zio.json.DecoderOps

import java.time.Clock

object LoginRoutes {

  // Secret Authentication key
  val SECRET_KEY = "secretKey"

  implicit val clock: Clock = Clock.systemUTC



  def buildClaim(text:String,lifeTime:Long)= JwtClaim {text}.issuedNow.expiresIn(lifeTime)

  // Helper to encode the JWT token
  def jwtEncode(username: String, liveSpan:Long): String = {
    //val json = s"""{"user": "${username}"}"""
    val json = s"""{${username}}"""
    val claim = JwtClaim {
      json
    }.issuedNow.expiresIn(liveSpan)
    Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)
  }

  // Helper to decode the JWT token
  def jwtDecode(token: String): Option[JwtClaim] = {
    val result = Jwt.decode(token, SECRET_KEY, Seq(JwtAlgorithm.HS512)).toOption
    println(s"jwtDecoded  ${result}")
    result
  }

  val invalidRequest = LoginRequest("InvalidUsername","InvalidPassword")

  def appLogin = Http.collectZIO[Request] {
    case req@Method.POST -> !! /"users"/"login" =>
      for {
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[LoginRequest])
              .mapError(e => new Throwable(e))
              .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
          ).either.flatMap(loginRequest => {
          val loginRequest_ = loginRequest.getOrElse(invalidRequest)
          val jwtEncode_ = jwtEncode(loginRequest_.password, 86400*365*20)
          UserRepository.getByUserName(loginRequest_.userName, "1000").either.map {
            case Right(user) =>checkLogin(loginRequest_,user)
            case Left(e) => Response.text(e.getMessage() + loginRequest_.userName + "/"
                                                         + loginRequest_.password).setStatus(Status.Unauthorized)
          }
        })
      } yield body

  }
  private def  checkLogin(loginRequest:LoginRequest,user:User):Response ={
    val content = jwtDecode(user.hash).toList.head.content;
    val pwd = content.substring(1, content.length - 1).replaceAll("\"", "")
    val pwdR = loginRequest.password
    val usernameR = loginRequest.userName
    val username = user.userName
    val check = ((usernameR == username) & (pwdR.substring(1, pwdR.length - 1) == pwd))
    if (check) {
      //val json = s"""{"${pwd}"}"""
      val encoded = user.hash //Jwt.encode(buildClaim(json, 1000000), SECRET_KEY, JwtAlgorithm.HS512)
      Response.text(user.copy(hash=encoded).toJson).addHeader(Header("authorization",encoded))
    } else {
      //ZIO.logInfo(s"Invalid  user name or password  ${username}")*>
      Response.text("Invalid  user name or password " + loginRequest.userName + "/" + loginRequest.password).setStatus(Status.Unauthorized)
    }
  }
}

