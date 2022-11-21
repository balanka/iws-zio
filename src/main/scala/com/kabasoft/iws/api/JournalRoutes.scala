package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain._
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import zio.http.model.{Method, Status}
import zio.json._

object JournalRoutes {

  val appJournal = Http.collectZIO[Request] {

      case Method.GET -> !! / "jou" =>
        JournalRepository
          .list("1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))

      case Method.GET -> !! / "jou" / id   =>
        JournalRepository.getBy(id, "1000").either.map {
          case Right(o) => Response.json(o.toJson)
          case Left(e)  => Response.text(e.getMessage() + "ID" + id)
        }
      case Method.GET -> !! / "jou" / id / from / to =>
        JournalRepository.find4Period(id, from.toInt, to.toInt, "1000")
          .runCollect
          .map(ch => Response.json(ch.toJson))
    }
}
