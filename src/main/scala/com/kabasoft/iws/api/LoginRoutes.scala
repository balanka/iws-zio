package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.{ loginRequestCodec, userCodec}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.common.DummyUser
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http._
import zio.http.Header.Custom
import zio.http.{Method, Status}
import zio.json.{DecoderOps, EncoderOps}

object LoginRoutes {
  private val defaultLifeSpan = 15*24*60*60L
  def appLogin = Http.collectZIO[Request] {

    case req@Method.POST -> Root / "users" / "login" =>
      for {
        _<-ZIO.logInfo(s"Request >>>>>>n ${req}")
        body <- req.body.asString
          .flatMap(request =>
            ZIO
              .fromEither(request.fromJson[LoginRequest])
              .mapError(e => RepositoryError(e))
              .tapError(e => ZIO.logInfo(s"Unparseable body ${e.message}"))
          )
          .flatMap (checkLogin)
      } yield body
  }

  private def checkLogin(loginRequest: LoginRequest): ZIO[UserRepository, RepositoryError, Response] = for {
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
    val env = System.getenv()
    val webUrl     =if (env.keySet().contains("IWS_WEB_URL")) env.get("IWS_WEB_URL") else "http://localhost:3000"
    println(s"webUrl >>>>>> $webUrl")
    if (check) {
      val json = s"""{"${loginRequest.password}"}"""
      val token = Utils.jwtEncode(json, defaultLifeSpan)
      Response.json(user.toJson).addHeader(Custom("authorization", token))
        .addHeader(Custom("Access-Control-Allow-Origin", "*"))
        .addHeader(Custom("Origin", webUrl))

    } else {
      Response.text("Invalid  user name or password "
        + loginRequest.userName + "/"
        + loginRequest.password).withStatus(Status.Unauthorized)
    }
  }
}
