package com.kabasoft.iws

import com.kabasoft.iws.api.LoginRoutes.jwtDecode
import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.User
import zio._
import zio.http.model.Headers.accessControlAllowOrigin
import zio.http.model.{Headers, Method}
import zio.http.{Body, Client}
import zio.json.DecoderOps

object AuthenticationClient extends ZIOAppDefault {

  /**
   * This example is trying to access a protected route in AuthenticationServer
   * by first making a login request to obtain a jwt token and use it to access
   * a protected route. Run AuthenticationServer before running this example.
   */

  val url         = "http://localhost:8091"
  // val data = s"""{"userName":"mady" ,"password":"wuduwali2x"}"""
  val data        = s"""{"userName":"bate2" ,"password":"wuduwali2x"}"""
  val defaultUser = User(-1, "NoUser", "", "", "", "", "", "")

  val program = for {

    // responsex <- Client.request(s"http://127.0.0.1:8091/bank", headers = Headers.bearerAuthorizationHeader(token))
    token <- Client.request(s"${url}/users/login", method = Method.POST, headers= accessControlAllowOrigin("*")
        ,content = Body.fromString(data)).flatMap(_.body.asString)
    _     <- ZIO.logInfo(s"token>> ${token}<<")
    // .flatMap(body =>
    r     <- ZIO
               .fromEither(token.fromJson[User])
               .mapError(e => new Throwable(e))
               .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
               .either
    token = r.getOrElse(defaultUser).hash
    _     <- Console.printLine(s"Token >>>>> ${token} >>>${jwtDecode(token).toList.head.content}")

    // Once the jwt token is procured, adding it as a Barer token in Authorization header while accessing a protected route.
    // response <- Client.request(s"${url}/user/userName/greet", headers = Headers.bearerAuthorizationHeader(token))
    //responsex <- Client.request(s"${url}/cust", method = Method.GET, headers = Headers.bearerAuthorizationHeader(token)++accessControlAllowOrigin("*"))
    responsex <- Client.request(s"${url}/bank/1000", headers = Headers.bearerAuthorizationHeader(token))//++accessControlAllowOrigin("*"))
    response <- Client.request(s"${url}/health2", headers = Headers.bearerAuthorizationHeader(token))
    //response <- Client.request(s"http://127.0.0.1:9090/health", headers = Headers.bearerAuthorizationHeader(token))
    // response <- Client.request(s"${url}/cust", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/sup", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/acc", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/cc", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/module", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/vat", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/user", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/ftr", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    // response <- Client.request(s"${url}/pac", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
    body     <- response.body.asString
    bodyx     <- responsex.body.asString
   // bodyy     <- responsey.status.code
    _        <- Console.printLine(":::acc>>>" + body )
    _        <- Console.printLine(":::bank>>>" + bodyx )
   // _        <- Console.printLine(":::health>>>" + responsey.status.code )
    //_        <- Console.printLine(":::>>>" + responsex.body.asChunk.map(_.toList) )
    //_        <- Console.printLine(":::>>>" + response.body.asString )
  } yield (println(":::>>>" + body ))

  override val run = program.provide(Client.default)

}
