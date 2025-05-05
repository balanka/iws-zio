package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.{loginRequestCodec, userCodec}
import com.kabasoft.iws.domain.*
import com.kabasoft.iws.repository.*
import zio.*
import zio.http.Header.Custom
import zio.http.*
import zio.json.{DecoderOps, EncoderOps}

object LoginRoutes:
  private val defaultLifeSpan = 15*24*60*60L
  def loginRoutes: Routes[UserRepository, Response] =
    Routes(
      Method.POST / "users" / "login" ->
        handler { (req: Request) =>
          for {
            loginRequest <- req.body.asString
              .flatMap(request => ZIO.logInfo(s"RequestX >>>>>>n ${request}")*>
                ZIO.fromEither(request.fromJson[LoginRequest])
              ).catchAll(e => ZIO.logInfo(s"Unparseable body: ${e.toString}")*>ZIO.succeed(LoginRequest.dummy))
            user <- UserRepository.getByUserName((loginRequest.userName, User.MODELID, loginRequest.company))
          } yield checkLogin(user, loginRequest)
        },
    ) @@ Middleware.debug

  private def checkLogin(user: User, loginRequest:LoginRequest): Response =
    
    println(s"checkLogin >>>>>> ${loginRequest.password}")
    println(s"pwd >>>>>> ${Utils.jwtEncode(loginRequest.password, defaultLifeSpan)}")
    //println(s"user >>>>>> $user")
    val pwd = Utils.jwtDecode(user.hash).get.subject.getOrElse("Subject")
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
      //val json = s""""$loginRequest.password""""
      val token = user.hash//Utils.jwtEncode(json, defaultLifeSpan)
      println(s"token >>>>>> $token")
      //Response.json(pwd).addHeader(Custom("authorization", token))
       Response.json(user.toJson).addHeader(Custom("authorization", token))
        .addHeader(Custom("Access-Control-Allow-Origin", "*"))
        .addHeader(Custom("Origin", webUrl))
    } else {
      Response.unauthorized("Invalid username or password.")
    }



