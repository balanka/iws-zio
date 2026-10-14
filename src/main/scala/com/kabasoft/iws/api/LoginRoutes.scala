package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol.{loginRequestCodec, userCodec}
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
import zio.http.Header.Custom
import zio.http._

import zio.json.{DecoderOps, EncoderOps}


object LoginRoutes:

  def loginRoutes: Routes[UserRepository, Response] =
    Routes(
      Method.POST / "users" / "login" ->
        handler { (req: Request) =>
          call(req)
        },
    ) @@ Middleware.debug

  private def sessionCookie(token: String): Cookie.Response =
    Cookie.Response(
      name = "iws_session",
      content = token,
      isHttpOnly = true,
      isSecure = true,
      sameSite = Some(Cookie.SameSite.Strict),
      path = Some(Path.root),
      maxAge = Some(8.hours)
    )

  private def call(req: Request) = {
    for {
      loginRequest <- req.body.asString
        .flatMap(request =>
          ZIO.fromEither(request.fromJson[LoginRequest])
        ).catchAll(e => ZIO.logInfo(s"Unparseable body: ${e.toString}") *> ZIO.succeed(LoginRequest.dummy))
      user <- UserRepository.getByUserName((loginRequest.userName, ModelId.USER.modelid, loginRequest.company))
    } yield checkLogin(user, loginRequest)
  }

  private def checkLogin(user: User, loginRequest: LoginRequest): Response =
    //val X = Utils.jwtEncode(loginRequest.password)
    val pwd = Utils.jwtDecode(user.hash).get.subject.getOrElse("Subject")
    val pwdR = loginRequest.password
    val usernameR = loginRequest.userName
    val username = user.userName
    val check = (usernameR == username) & (pwdR == pwd)
    //val iwsWeb = scala.util.Properties.envOrElse("IWS_WEB_HOST", "http://127.0.0.1")
    //val webUrl = scala.util.Properties.envOrElse("IWS_WEB_HOST", "http://192.168.64.1")

    if (check) {
      val token = user.hash
      Response
        .json(user.toJson)
        .addHeader(Custom("authorization", token))
        .addCookie(sessionCookie(token))
    } else {
      Response.unauthorized("Invalid username or password.")
    }