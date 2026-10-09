package com.kabasoft.iws.api

import zio.http._
import java.nio.charset.StandardCharsets

object HtmxResponse:

  private val htmlHeaders =
    Headers(
      Header.Custom("Content-Type", "text/html; charset=UTF-8")
    )

  def html(body: String): Response =
    Response(
      status = Status.Ok,
      headers = htmlHeaders,
      body = Body.fromString(body, StandardCharsets.UTF_8)
    )

  def errorHtml(message: String): Response =
    Response(
      status = Status.InternalServerError,
      headers = htmlHeaders,
      body = Body.fromString(s"""<p style="color:red">Error: $message</p>""", StandardCharsets.UTF_8)
    )

  def parseForm(body: String): Map[String, String] =
    body.split("&").flatMap { kv =>
      val i = kv.indexOf('=')
      if (i < 0) None
      else Some(
        java.net.URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8) ->
          java.net.URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8)
      )
    }.toMap