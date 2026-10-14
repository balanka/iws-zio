package com.kabasoft.iws.api

import zio._
import zio.http._
import com.kabasoft.iws.domain.AppError.AuthenticationError

object SessionCookie:
  val name = "iws_session"

  def tokenFrom(req: Request): Option[String] =
    req.cookie(name).map(_.content)

  def requireSession(req: Request): ZIO[Any, AuthenticationError, String] =
    ZIO.fromOption(tokenFrom(req))
      .orElseFail(AuthenticationError("Missing iws_session cookie", 0))