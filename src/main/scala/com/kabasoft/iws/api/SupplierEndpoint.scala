package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.supplierschema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Supplier
import com.kabasoft.iws.repository._

import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status

object SupplierEndpoint {

  val supCreateAPI      = Endpoint.post("sup").in[Supplier].out[Int].outError[RepositoryError](Status.InternalServerError)
  val supAllAPI         = Endpoint.get("sup").out[List[Supplier]].outError[RepositoryError](Status.InternalServerError)
  val supByIdAPI        = Endpoint.get("sup" / string("id")).out[Supplier].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.get("sup" / string("id")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val supCreateEndpoint      = supCreateAPI.implement(sup => SupplierRepository.create(List(sup)).mapError(e => RepositoryError(e.getMessage)))
  val supAllEndpoint         = supAllAPI.implement(_ => SupplierRepository.all("1000").mapError(e => RepositoryError(e.getMessage)))
  val supByIdEndpoint        = supByIdAPI.implement(id => SupplierRepository.getBy(id, "1000").mapError(e => RepositoryError(e.getMessage)))
  private val deleteEndpoint = deleteAPI.implement(id => SupplierRepository.delete(id, "1000").mapError(e => RepositoryError(e.getMessage)))

   val routesSup = supAllEndpoint ++ supByIdEndpoint ++ supCreateEndpoint ++deleteEndpoint

  val appSup = routesSup//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ supCreateEndpoint
}
