package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.model.{Method, Status}
import zio.json._

object BankStmtRoutes {

  val appBankStmt = Http.collectZIO[Request] {

      case Method.GET -> !! / "bs" =>
        BankStatementRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

    }
}
