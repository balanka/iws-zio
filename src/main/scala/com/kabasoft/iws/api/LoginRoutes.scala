package com.kabasoft.iws.api

import java.time.Clock
import scala.util.Try
import zio.*
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import com.kabasoft.iws.api.Protocol.{loginRequestCodec, userCodec}
import com.kabasoft.iws.api.Utils.bearerAuthWithContext
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.domain.common.DummyUser
import com.kabasoft.iws.repository.*
import zio.ZIO
import zio.http.*
import zio.http.Header.Custom
import zio.http.Method
import zio.json.{DecoderOps, EncoderOps}

object LoginRoutes:
  private val defaultLifeSpan = 15*24*60*60L
//  implicit val clock: Clock = Clock.systemUTC

  // Secret Authentication key
//  val SECRET_KEY = "secretKey"
//
//  def jwtEncode(username: String, key: String): String =
//    Jwt.encode(JwtClaim(subject = Some(username)).issuedNow.expiresIn(300), key, JwtAlgorithm.HS512)
//
//  def jwtDecode(token: String, key: String): Try[JwtClaim] =
//    Jwt.decode(token, key, Seq(JwtAlgorithm.HS512))

  def loginRoutes: Routes[UserRepository, Response] =
    Routes(
      // A route that is accessible only via a jwt token
      Method.GET / "profile" / "me" -> handler { (_: Request) =>
        ZIO.serviceWith[String](name => Response.text(s"Welcome $name!"))
      } @@ bearerAuthWithContext,

      // A login route that is successful only if the password is the reverse of the username
      Method.POST / "users" / "login" ->
        handler { (req: Request) =>
          for {
            _ <- ZIO.logInfo(s"Request >>>>>>n ${req}")
            loginRequest <- req.body.asString
              .flatMap(request => ZIO.logInfo(s"RequestX >>>>>>n ${request}")*>
                ZIO.fromEither(request.fromJson[LoginRequest])
              ).catchAll(e => ZIO.logInfo(s"Unparseable body: ${e.toString}")*>ZIO.succeed(LoginRequest.dummy))
            user <- UserRepository.getByUserName((loginRequest.userName, User.MODELID, loginRequest.company))
          } yield checkLogin(user, loginRequest)
        },
    ) @@ Middleware.debug

  private def checkLogin(user: User, loginRequest:LoginRequest): Response =  {
    
    println(s"checkLogin >>>>>>")
    println(s"pwd >>>>>> ${Utils.jwtEncode(loginRequest.password, defaultLifeSpan)}")
    println(s"user >>>>>> $user")
    val pwd = Utils.jwtDecode(user.hash).toList.head.content.replace("{","").replace("}","")
    val pwdR = loginRequest.password
    val usernameR = loginRequest.userName
    val username = user.userName
    val check = (usernameR == username) & (pwdR == pwd)
    println(s"check >>>>>> $check")
    println(s"pwd >>>>>> $pwd")
    val webUrl = scala.util.Properties.envOrElse("IWS_WEB_URL", "http://localhost:3000")
    //if (env.keySet().contains("IWS_WEB_URL")) env.get("IWS_WEB_URL") else "http://localhost:3000"
    println(s"webUrl >>>>>> $webUrl")
    if (check) {
      val json = s""""$loginRequest.password""""
      val token = user.hash//Utils.jwtEncode(json, defaultLifeSpan)
      println(s"token >>>>>> $token")
      //Response.json(pwd).addHeader(Custom("authorization", token))
      val  r = Response.json(user.toJson).addHeader(Custom("authorization", token))
        .addHeader(Custom("Access-Control-Allow-Origin", "*"))
        .addHeader(Custom("Origin", webUrl))
      println(s"Response headers >>>>>> ${r.headers}")
      r

    } else {
      Response.unauthorized("Invalid username or password.")
    }
  }

  private def checkLoginX(loginRequest: LoginRequest): ZIO[UserRepository, RepositoryError, Response] = for {
    _ <- ZIO.logInfo(s"checkLogin >>>>>>")
    _ <- ZIO.logInfo(s"pwd >>>>>> ${Utils.jwtEncode(loginRequest.password,defaultLifeSpan)}")
    r <- UserRepository.all((User.MODELID, loginRequest.company))
    user = r.find(_.userName.equals(loginRequest.userName)).getOrElse(DummyUser)
    _ <- ZIO.logDebug(s"all Users >>>>>> ${user}")
    content   = Utils.jwtDecode(user.hash).toList.head.content.replace("{","").replace("}","")

  } yield {

    //println(s"content >>>>>> $content")
    val pwd       = content
    val pwdR      = loginRequest.password
    val usernameR = loginRequest.userName
    val username  = user.userName
    val check     = (usernameR == username) & (pwdR == pwd)
    val webUrl     = scala.util.Properties.envOrElse("IWS_WEB_URL", "http://localhost:3000")
    //if (env.keySet().contains("IWS_WEB_URL")) env.get("IWS_WEB_URL") else "http://localhost:3000"
    println(s"webUrl >>>>>> $webUrl")
    if (check) {
      val json = s"""{"${loginRequest.password}"}"""
      val token = Utils.jwtEncode(json, defaultLifeSpan)
      Response.json(user.toJson).addHeader(Custom("authorization", token))
        .addHeader(Custom("Access-Control-Allow-Origin", "*"))
        .addHeader(Custom("Origin", webUrl))

    } else {
      Response.unauthorized("Invalid username or password.")
    }
  }

