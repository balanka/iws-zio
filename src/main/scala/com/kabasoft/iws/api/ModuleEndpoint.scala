package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.moduleSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Module
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status

object ModuleEndpoint {

  val moduleCreateAPI      = Endpoint.post("module").in[Module].out[Int].outError[RepositoryError](Status.InternalServerError)
  val moduleAllAPI         = Endpoint.get("module"/ string("company")).out[List[Module]].outError[RepositoryError](Status.InternalServerError)
  val moduleByIdAPI        = Endpoint.get("module" / string("id")/ string("company")).out[Module].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI    = Endpoint.delete("module" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val moduleCreateEndpoint   = moduleCreateAPI.implement(m => ModuleRepository.create(List(m)).mapError(e => RepositoryError(e.getMessage)))
  val moduleAllEndpoint      = moduleAllAPI.implement(company => ModuleRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  val moduleByIdEndpoint     = moduleByIdAPI.implement(p => ModuleRepository.getBy(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
  val moduleDeleteEndpoint = deleteAPI.implement(p => ModuleRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
   val routesModule    = moduleAllEndpoint ++ moduleByIdEndpoint ++ moduleCreateEndpoint ++moduleDeleteEndpoint

  val appModule= routesModule//.toApp//@@ bearerAuth(jwtDecode(_).isDefined)  ++ moduleCreateEndpoint

}
