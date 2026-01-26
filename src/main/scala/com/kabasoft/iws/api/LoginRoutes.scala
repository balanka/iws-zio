package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.{loginRequestCodec, userCodec}
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
import zio.http.Header.Custom
import zio.http._

import zio.json.{DecoderOps, EncoderOps}

object LoginRoutes:
  private val defaultLifeSpan =  3*365*24*60*60L // 3 years
  def loginRoutes: Routes[UserRepository, Response] =
    Routes(
      Method.POST / "users" / "login" ->
        handler { (req: Request) =>
          for {
            loginRequest <- req.body.asString
              .flatMap(request => //ZIO.logInfo(s"RequestX >>>>>>n ${request}")*>
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
    val x= scala.util.Properties.envOrElse("IWS_WEB_URL", s"X")
    println(s"webUrl >>>>>> $x")
    println(s"pwd >>>>>> $pwd")
    val webUrl = scala.util.Properties.envOrElse("IWS_WEB_URL", "http://localhost:3000")
   // val webUrl = scala.util.Properties.envOrElse("IWS_WEB_URL", "http://127.0.0.1:3000")
    //val webUrl = scala.util.Properties.envOrElse("IWS_WEB_URL", s"http://localhost:5173")
    //if (env.keySet().contains("IWS_WEB_URL")) env.get("IWS_WEB_URL") else "http://localhost:3000"
    println(s"webUrl >>>>>> $webUrl")
    if (check) {
      //val json = s""""$loginRequest.password""""
      val token = user.hash//Utils.jwtEncode(json, defaultLifeSpan)
      println(s"token >>>>>> $token")
       Response.json(user.toJson).addHeader(Custom("authorization", token))
        //.addHeader(Custom("Access-Control-Allow-Origin", "*"))
        .addHeader(Custom("Access-Control-Allow-Origin", webUrl))
    } else {
      Response.unauthorized("Invalid username or password.")
    }



