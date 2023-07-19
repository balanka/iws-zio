package com.kabasoft.iws

import com.kabasoft.iws.api.LoginRoutes.jwtDecode
import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.User
import zio._
import zio.http.{Body, Client, Header, Headers, Method}
import zio.json.DecoderOps


object AuthenticationClient extends ZIOAppDefault {

  /**
   * This example is trying to access a protected route in AuthenticationServer
   * by first making a login request to obtain a jwt token and use it to access
   * a protected route. Run AuthenticationServer before running this example.
   */

  val url         = "http://mac-studio:8091"
  // val data = s"""{"userName":"mady" ,"password":"wuduwali2x"}"""
  val data        = s"""{"userName":"bate2" ,"password":"wuduwali2x"}"""
  val defaultUser = User(-1, "NoUser", "", "", "", "", "", "")

  val program = for {

    response <- Client.request(url =s"${url}/users/login", method = Method.POST
        , headers= Headers.apply(zio.http.Header.AccessControlAllowHeaders.All)
        , content = Body.fromString(data)).flatMap(_.body.asString)
    _     <- ZIO.logInfo(s"token>> ${response}<<")
    // .flatMap(body =>
    r     <- ZIO.fromEither(response.fromJson[User])
               .mapError(e => new Throwable(e))
               .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
               .either
    token = r.getOrElse(defaultUser).hash
    _     <- Console.printLine(s"Token >>>>> ${token} >>>${jwtDecode(token).toList.head.content}")

    // Once the jwt token is procured, adding it as a Barer token in Authorization header while accessing a protected route.
 //   response0 <- Client.request(s"${url}/bank/1000", headers = Headers.bearerAuthorizationHeader(token))//++accessControlAllowOrigin("*"))
//    response1 <- Client.request(s"${url}/health2", headers = Headers.bearerAuthorizationHeader(token))
//    response2 <- Client.request(s"${url}/health", headers = Headers.bearerAuthorizationHeader(token))
//     response3 <- Client.request(s"${url}/cust/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
//     response4 <- Client.request(s"${url}/sup/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
//     response5 <- Client.request(s"${url}/acc/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
//     response6 <- Client.request(s"${url}/cc/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
//     response7 <- Client.request(s"${url}/module/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
//     response8 <- Client.request(s"${url}/vat/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
//     response9 <- Client.request(s"${url}/user/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
     response10 <- Client.request(s"${url}/ftr/model/1000/114", headers = Headers(Header.Authorization.Bearer(token))) //(r.getOrElse(defaultUser).hash))
     //response <- Client.request(s"${url}/pac/1000", headers = Headers.bearerAuthorizationHeader(r.getOrElse(defaultUser).hash))
 //   body     <- response0.body.asString
//    body1     <- response1.body.asString
//    body2     <- response2.body.asString
//    body3     <- response3.body.asString
//    body4     <- response4.body.asString
//    body5     <- response5.body.asString
//    body6     <- response6.body.asString
//    body7     <- response7.body.asString
//    body8     <- response8.body.asString
//    body9     <- response9.body.asString
    body10     <- response10.body.asString
//    _        <- Console.printLine(":::bank>>>" + body )
//    _        <- Console.printLine(":::acc>>>" + body1 )
//    _        <- Console.printLine(":::bank>>>" + body2 )
//    _        <- Console.printLine(":::Customer>>>" + body3 )
//    _        <- Console.printLine(":::Supplier>>>" + body4 )
//    _        <- Console.printLine(":::account>>>" + body5 )
//    _        <- Console.printLine(":::Costcenter>>>" + body6 )
//    _        <- Console.printLine(":::bank>>>" + body7 )
//    _        <- Console.printLine(":::bank>>>" + body8 )
//    _        <- Console.printLine(":::bank>>>" + body9 )
    _        <- Console.printLine(":::ftr>>>" + body10 )


  } yield (println(":::>>>" + body10 ))

  override val run = program.provide(Client.default)

}

