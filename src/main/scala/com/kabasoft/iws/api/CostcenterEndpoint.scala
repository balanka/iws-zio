package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.costcenterSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Costcenter
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status


object CostcenterEndpoint {

  val ccCreateAPI          = Endpoint.post("cc").in[Costcenter].out[Int].outError[RepositoryError](Status.InternalServerError)
  val ccAllAPI          = Endpoint.get("cc").out[List[Costcenter]].outError[RepositoryError](Status.InternalServerError)
  val ccByIdAPI         = Endpoint.get("cc" /string("id")).out[Costcenter].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.get("cc" / string("id")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val ccCreateEndpoint       = ccCreateAPI.implement(cc => CostcenterRepository.create(List(cc)).mapError(e => RepositoryError(e.getMessage)))
  val ccAllEndpoint          = ccAllAPI.implement(_ => CostcenterRepository.all("1000").mapError(e => RepositoryError(e.getMessage)))
  val ccByIdEndpoint         = ccByIdAPI.implement(id => CostcenterRepository.getBy(id, "1000").mapError(e => RepositoryError(e.getMessage)))
   val deleteEndpoint = deleteAPI.implement(id => CostcenterRepository.delete(id, "1000").mapError(e => RepositoryError(e.getMessage)))

   val routesCC = ccAllEndpoint ++ ccByIdEndpoint ++ ccCreateEndpoint ++deleteEndpoint

  val appCC= routesCC//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ ccCreateEndpoint

}
