package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.customerSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Customer
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec.{string, _}
import zio.http.endpoint.Endpoint
import zio.http.Status
import zio.schema.DeriveSchema.gen

object CustomerEndpoint {

  val custCreateAPI      = Endpoint.post("cust").in[Customer].out[Customer].outError[RepositoryError](Status.InternalServerError)
  val custAllAPI         = Endpoint.get("cust"/ string("company")).out[List[Customer]].outError[RepositoryError](Status.InternalServerError)
  val custByIdAPI        = Endpoint.get("cust" / string("id")/ string("company")).out[Customer].outError[RepositoryError](Status.InternalServerError)
  val custModifyAPI     = Endpoint.put("cust").in[Customer].out[Customer].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI  = Endpoint.delete("cust" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val custCreateEndpoint     = custCreateAPI.implement (cust=> ZIO.logInfo(s"Create customer  ${cust}") *>CustomerRepository.create(cust).mapError(e => RepositoryError(e.getMessage)))
  val custAllEndpoint        = custAllAPI.implement (company=> CustomerCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val custByIdEndpoint       = custByIdAPI.implement(p => CustomerCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val ccModifyEndpoint = custModifyAPI.implement(p => ZIO.logInfo(s"Modify customer  ${p}") *>
    CustomerRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    CustomerRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
   val custDeleteEndpoint = deleteAPI.implement(p => CustomerRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routesCust = custAllEndpoint ++ custByIdEndpoint ++ custCreateEndpoint ++custDeleteEndpoint ++ccModifyEndpoint

  val appCust= routesCust//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ custCreateEndpoint
}
