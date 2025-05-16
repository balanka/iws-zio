package com.kabasoft.iws.api

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import zio.ZIO
import zio.http.{Handler, HandlerAspect, Header, Headers, Request, Response}
import java.time.Clock
import scala.util.Try

object Utils {
  private val defaultLifeSpan = 15*24*60*60L
  private val SECRET_KEY = "secretKey"
  //private val SECRET_KEY = "PqRwLF2rhHe8J22oBeHy_9"
  private val never = Long.MaxValue
  implicit val clock: Clock = Clock.systemUTC

  def jwtEncode(username: String, liveSpan: Long, key: String = SECRET_KEY): String =
    Jwt.encode(JwtClaim(subject = Some(username)).issuedNow.expiresIn(liveSpan), key, JwtAlgorithm.HS512)

//  def jwtEncode(word: String, liveSpan: Long): String =
//    println(s"encoding word  $word")
//    val json  = s"""{$word}"""
//    val liveSpan_ = if (liveSpan == -1L) never else liveSpan
//    val claim = JwtClaim {json}.issuedNow.expiresIn(liveSpan_)
//    Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)


  def jwtDecode(token: String): Option[JwtClaim] =
    val result = Jwt.decode(token, SECRET_KEY, Seq(JwtAlgorithm.HS512)).toOption
    println(s"jwtDecodedX  $result")
    result

  def jwtDecode(token: String, key: String): Try[JwtClaim] =
    val result = Jwt.decode(token, key, Seq(JwtAlgorithm.HS512))
    println(s"jwtDecodedY  $result")
    result

  val bearerAuthWithContext: HandlerAspect[Any, String] =
    HandlerAspect.interceptIncomingHandler(Handler.fromFunctionZIO[Request] { request =>
      request.header(Header.Authorization) match
        case Some(Header.Authorization.Bearer(token)) =>
          ZIO.logInfo(s"token.value.asString >>>>>>${token.value.asString}")*>
          ZIO
            .fromTry(jwtDecode(token.value.asString, SECRET_KEY))
            .orElseFail(Response.badRequest(s"Invalid or expired token! 400  value:${token.value.asString} "))
            .flatMap(claim => ZIO.logInfo(s"claim >>>>>>n ${claim}")*>ZIO.fromOption(claim.subject)
              .orElseFail(Response.badRequest(s"Missing subject claim! 400 ${claim}")))
            .map(u => (request, u))

        case _ => ZIO.fail(Response.unauthorized.addHeaders(Headers(Header.WWWAuthenticate.Bearer(realm = "Access"))))
    })
}
