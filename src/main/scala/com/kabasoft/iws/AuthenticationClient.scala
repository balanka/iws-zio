package com.kabasoft.iws

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.User
import zio._
import zio.http.model.{Headers, Method}
import zio.http.{Body, Client}
import zio.json.DecoderOps

object AuthenticationClient extends ZIOAppDefault {

  /**
   * This example is trying to access a protected route in AuthenticationServer
   * by first making a login request to obtain a jwt token and use it to access
   * a protected route. Run AuthenticationServer before running this example.
   */
  val url = "http://127.0.0.1:9090"
  //val data = s"""{"userName":"mady" ,"password":"wuduwali2x"}"""
  val data = s"""{"userName":"bate2" ,"password":"wuduwali2x"}"""
  val defaultUser = User(-1,"NoUser", "","","","","","")



  val program = for {
    // Making a login request to obtain the jwt token. In this example the password should be the reverse string of username.
    //token    <- Client.request(s"${url}/login/username/emanresu").flatMap(_.body.asString)
    token    <- Client.request(s"${url}/users/login", method= Method.POST, content=Body.fromString(data)).flatMap(_.body.asString)
    _        <- ZIO.logInfo(s"token>> ${token}<<")
     // .flatMap(body =>
    r<-       ZIO.fromEither(token.fromJson[User])
          .mapError(e => new Throwable(e))
          .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
      .either//.flatMap(user_ => user_.getOrElse(User(-1,"NoUser", "","","","","","")))
    _        <- Console.printLine(r.getOrElse(defaultUser).hash)

    // Once the jwt token is procured, adding it as a Barer token in Authorization header while accessing a protected route.
    //response <- Client.request(s"${url}/user/userName/greet", headers = Headers.bearerAuthorizationHeader(token))

    //response <- Client.request(s"${url}/bank", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    //response <- Client.request(s"${url}/cust", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    //response <- Client.request(s"${url}/sup", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
   // response <- Client.request(s"${url}/acc", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    //response <- Client.request(s"${url}/cc", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
   // response <- Client.request(s"${url}/module", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
   // response <- Client.request(s"${url}/vat", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
   // response <- Client.request(s"${url}/user", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
   // response <- Client.request(s"${url}/ftr", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    response <- Client.request(s"${url}/pac", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    body     <- response.body.asString
    _        <- Console.printLine(body)
  } yield ()

  override val run = program.provide(Client.default)

}
