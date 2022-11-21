package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.Bank._
import com.kabasoft.iws.repository.BankRepository
import zio._
import zio.http.api.HttpCodec.{literal, string}
import zio.http.api.{EndpointSpec, RouteCodec}

import java.time.Instant

object BankEndpoint {
  implicit def stringToIn(s: String): RouteCodec[Unit] = RouteCodec.literal(s)

  val getBankAll = EndpointSpec.get(literal("bank") ).out[List[Bank]]
  val allBankAPI = EndpointSpec.get[Unit]("bank").out[List[Bank]]
  val getBankById = EndpointSpec.get(literal("bank") / string).out[Bank]
  val bankByIdAPI = EndpointSpec.get[String]("bank"/ RouteCodec.string ).out[Bank]

  val getBankAllEndpoint = getBankAll.implement { case () =>BankRepository.all("1000")}
  val allBankHandler = allBankAPI.implement { case () => BankRepository.all("1000")}

  val getBankByIdEndpoint = getBankById.implement { case (id: String) =>BankRepository.getBy(id,"1000")}

  val banks = List(Bank("4711", "title", "body", Instant.now(), Instant.now(), Instant.now(),11,"1000"))

  val bankByIdHandler = bankByIdAPI.implement { case (id) =>
    ZIO.succeed(Bank(id, "title", "body", Instant.now(), Instant.now(), Instant.now(), 11, "1000"))
  }


}
