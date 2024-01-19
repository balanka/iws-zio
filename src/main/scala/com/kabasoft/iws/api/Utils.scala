package com.kabasoft.iws.api

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}
import java.time.Clock

object Utils {
  private val SECRET_KEY = "secretKey"
  //private val SECRET_KEY = "PqRwLF2rhHe8J22oBeHy_9"
  private val never = Long.MaxValue
  implicit val clock: Clock = Clock.systemUTC

  def jwtEncode(username: String, liveSpan: Long): String = {
    val json  = s"""{$username}"""
    val liveSpan_ = if (liveSpan == -1L) never else liveSpan
    val claim = JwtClaim {json}.issuedNow.expiresIn(liveSpan_)
    Jwt.encode(claim, SECRET_KEY, JwtAlgorithm.HS512)
  }

  def jwtDecode(token: String): Option[JwtClaim] = {
    val result = Jwt.decode(token, SECRET_KEY, Seq(JwtAlgorithm.HS512)).toOption
    println(s"jwtDecoded  $result")
    result
  }

}
