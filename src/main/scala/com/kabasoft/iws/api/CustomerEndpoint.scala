package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.customerSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Customer
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status
import zio.schema.DeriveSchema.gen

object CustomerEndpoint {

  val custCreateAPI         = Endpoint.post("cust").in[Customer].out[Int].outError[RepositoryError](Status.InternalServerError)
  val custAllAPI         = Endpoint.get("cust").out[List[Customer]].outError[RepositoryError](Status.InternalServerError)
  val custByIdAPI        = Endpoint.get("cust" / string("id")).out[Customer].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI  = Endpoint.get("cust" / string("id")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val custCreateEndpoint     = custCreateAPI.implement (cust=> CustomerRepository.create(List(cust)).mapError(e => RepositoryError(e.getMessage)))
  val custAllEndpoint        = custAllAPI.implement (_=> CustomerRepository.all("1000").mapError(e => RepositoryError(e.getMessage)))
  val custByIdEndpoint       = custByIdAPI.implement(id => CustomerRepository.getBy(id, "1000").mapError(e => RepositoryError(e.getMessage)))
   val deleteEndpoint = deleteAPI.implement(id => CustomerRepository.delete(id, "1000").mapError(e => RepositoryError(e.getMessage)))

  val routesCust = custAllEndpoint ++ custByIdEndpoint ++ custCreateEndpoint ++deleteEndpoint

  val appCust= routesCust//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ custCreateEndpoint
}
