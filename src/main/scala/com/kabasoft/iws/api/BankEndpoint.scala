package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{bankSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status

object BankEndpoint {

  val bankCreateAPI     = Endpoint.post("bank").in[Bank].out[Int].outError[RepositoryError](Status.InternalServerError)
  val bankAllAPI        = Endpoint.get("bank" / string("company")).out[List[Bank]].outError[RepositoryError](Status.InternalServerError)
  val bankByIdAPI       = Endpoint.get("bank" / string("id")/ string("company")).out[Bank].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("bank" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val bankAllEndpoint        = bankAllAPI.implement(company => BankRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  val bankCreateEndpoint = bankCreateAPI.implement(bank =>
    ZIO.logDebug(s"Insert bank  ${bank}") *>
    BankRepository.create(List(bank)).mapError(e => RepositoryError(e.getMessage)))
  val bankByIdEndpoint = bankByIdAPI.implement( p => BankRepository.getBy(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
  val bankDeleteEndpoint = deleteAPI.implement(p => BankRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = bankAllEndpoint ++ bankByIdEndpoint  ++ bankCreateEndpoint ++bankDeleteEndpoint//++ bankCreateEndpoint)

  val appBank = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
