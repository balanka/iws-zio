package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{bankSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status

object BankEndpoint {

  val bankCreateAPI     = Endpoint.post("bank").in[Bank].out[Int].outError[RepositoryError](Status.InternalServerError)
  val bankAllAPI        = Endpoint.get("bank" / string("company")).out[List[Bank]].outError[RepositoryError](Status.InternalServerError)
  val bankByIdAPI       = Endpoint.get("bank" / string("id")).out[Bank].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.get("bank" / string("id")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val bankAllEndpoint        = bankAllAPI.implement(company => BankRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  val bankCreteEndpoint = bankCreateAPI.implement(bank => BankRepository.create(List(bank)).mapError(e => RepositoryError(e.getMessage)))
  val bankByIdEndpoint = bankByIdAPI.implement(id => BankRepository.getBy(id, "1000").mapError(e => RepositoryError(e.getMessage)))
  private val deleteEndpoint = deleteAPI.implement(id => BankRepository.delete(id, "1000").mapError(e => RepositoryError(e.getMessage)))

  val routes = bankAllEndpoint ++ bankByIdEndpoint  ++ bankCreteEndpoint ++deleteEndpoint//++ bankCreateEndpoint)

  val appBank = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
