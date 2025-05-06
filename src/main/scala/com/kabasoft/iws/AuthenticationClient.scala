package com.kabasoft.iws
import com.kabasoft.iws.api.Protocol.given
import com.kabasoft.iws.api.Utils
import com.kabasoft.iws.domain.User
import zio._
import zio.http._
import zio.json.DecoderOps

object AuthenticationClient extends ZIOAppDefault {

  /**
   * This example is trying to access a protected route in AuthenticationServer
   * by first making a login request to obtain a jwt token and use it to access
   * a protected route. Run AuthenticationServer before running this example.
   */
  //val url = "http://localhost:8080"
  //val url = "http://mac-studio:8091"
  val url = "http://localhost:8091"
  val data        = s"""{"userName":"bate2" ,"password":"Wuduwali2x", "company":"1000","language":"en"}"""
  val defaultUser = User(-1, "NoUser", "", "", "", "", "", "")

  //val loginUrl = URL.decode(s"${url}/login").toOption.get
  //val greetUrl = URL.decode(s"${url}/profile/me").toOption.get
  val loginUrl = URL.decode(s"${url}/users/login").toOption.get
  def getUrl (ctx:String, modelid:Int) = URL.decode(s"$url/$ctx/$modelid/1000").toOption.get
  def getUrl2 (ctx:String, modelid:Int) = URL.decode(s"$url/$ctx/$modelid").toOption.get
  def getUrl3 (ctx:String, modelid:Int, company:String) = URL.decode(s"$url/$ctx/$company/$modelid").toOption.get

  val program = for {
    client   <- ZIO.service[Client]
    // Making a login request to obtain the jwt token. In this example the password should be the reverse string of username.
    response     <- client
      .batched(Request.post(loginUrl, Body.fromString(data)).addHeader(zio.http.Header.AccessControlAllowHeaders.All))
      .flatMap(_.body.asString)
     r    <- ZIO.fromEither(response.fromJson[User])
                   .mapError(e => new Throwable(e))
                   .tapError(e => ZIO.logInfo(s"Unparseable body ${e}"))
                   .either
        token = r.getOrElse(defaultUser).hash
        decoded = Utils.jwtDecode(token).get.subject.getOrElse("Subject")
        encoded = Utils.jwtEncode(decoded, 15*24*60*60L)
        decoded2 = Utils.jwtDecode(encoded).get.subject.getOrElse("Subject")
    //_<- ZIO.logInfo(s"User >>>>>>n ${r.getOrElse(defaultUser)}")
    _<- ZIO.logInfo(s"decoded token >>>>>>n $decoded")
    _<- ZIO.logInfo(s"encoded >>>>>>n $encoded")
    _<- ZIO.logInfo(s"decoded2 >>>>>>n $decoded2")
    _<- ZIO.logInfo(s"token >>>>>>n $token")
    // Authorization header while accessing a protected route.
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("sup", 1)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("cust", 3)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("mf", 6)))
    //response <- client.addHeader(Header.Authorization.Bearer(token)).batched(Request.get(getUrl("acc", 9)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl2("comp", 10)))
    //response <- client.addHeader(Header.Authorization.Bearer(token)).batched(Request.get(getUrl3("comp", 10, "1000")))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("mf",11)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("vat", 14)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("asset", 19)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("emp", 33)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("art", 34)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("store", 35)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("mf", 36)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("pac", 106)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ltr", 104)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ltr", 105)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ltr", 106)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ltr", 110)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("user", 111)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("role", 121)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ftr", 112)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ftr", 114)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ftr", 124)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ftr",134)))
    response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ftr", 136)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ftr",144)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("ftr",122)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("perm",141)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("fmodule",151)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("s_item",171)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("payrollTax",172)))
    //response <- client.addHeader(Header.Authorization.Bearer(Utils.jwtEncode(decoded, 15*24*60*60L))).batched(Request.get(getUrl("module", 400)))

    body     <- response.body.asString

    _        <- Console.printLine(body)
  } yield ()

  override val run = program.provide(Client.default)

}
