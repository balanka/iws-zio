package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{costcenterSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.Costcenter
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.Status


object CostcenterEndpoint {

  val ccCreateAPI          = Endpoint.post("cc").in[Costcenter].out[Costcenter].outError[RepositoryError](Status.InternalServerError)
  val ccAllAPI          = Endpoint.get("cc"/ string("company")).out[List[Costcenter]].outError[RepositoryError](Status.InternalServerError)
  val ccByIdAPI         = Endpoint.get("cc" /string("id")/ string("company")).out[Costcenter].outError[RepositoryError](Status.InternalServerError)
  val ccModifyAPI     = Endpoint.put("cc").in[Costcenter].out[Costcenter].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("cc" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val ccCreateEndpoint       = ccCreateAPI.implement(cc => CostcenterRepository.create(cc).mapError(e => RepositoryError(e.getMessage)))
  val ccAllEndpoint          = ccAllAPI.implement(company => CostcenterCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val ccByIdEndpoint         = ccByIdAPI.implement(p => CostcenterCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val ccModifyEndpoint       = ccModifyAPI.implement(p => ZIO.logInfo(s"Modify costcenter  ${p}") *>
    CostcenterRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    CostcenterRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
   val ccDeleteEndpoint = deleteAPI.implement(p => CostcenterRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

   val routesCC = ccAllEndpoint ++ ccByIdEndpoint ++ ccCreateEndpoint ++ccDeleteEndpoint++ccModifyEndpoint

  val appCC= routesCC//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ ccCreateEndpoint

}
