package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Fmodule
import com.kabasoft.iws.repository.Schema.{fmoduleSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object FModuleEndpoint {

  val fmoduleCreateAPI     = Endpoint.post("fmodule").in[Fmodule].out[Fmodule].outError[RepositoryError](Status.InternalServerError)
  val fmoduleAllAPI        = Endpoint.get("fmodule" / int("modelid")/string("company")).out[List[Fmodule]].outError[RepositoryError](Status.InternalServerError)
  val fmoduleByIdAPI       = Endpoint.get("fmodule" / string("id")/ string("company")).out[Fmodule].outError[RepositoryError](Status.InternalServerError)
  val fmoduleModifyAPI     = Endpoint.put(literal("fmodule")).in[Fmodule].out[Fmodule].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("fmodule" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val fmoduleAllEndpoint        = fmoduleAllAPI.implement(p => ZIO.logInfo(s"get all module for   ${p}")
    *>FModuleRepository.all(p).mapError(e => RepositoryError(e.getMessage)))
  val fmoduleCreateEndpoint = fmoduleCreateAPI.implement(perm =>
    ZIO.logInfo(s"Insert module  ${perm}") *>
      FModuleRepository.create(perm).mapError(e => RepositoryError(e.getMessage)))
  val fmoduleByIdEndpoint = fmoduleByIdAPI.implement( p => FModuleRepository.getBy((p._1.toInt, p._2)).mapError(e => RepositoryError(e.getMessage)))
  val fmoduleModifyEndpoint = fmoduleModifyAPI.implement(p => ZIO.logInfo(s"Modify module  ${p}") *>
    FModuleRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    FModuleRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val fmoduleDeleteEndpoint = deleteAPI.implement(p => FModuleRepository.delete(p._1.toInt, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = fmoduleAllEndpoint ++ fmoduleByIdEndpoint  ++ fmoduleCreateEndpoint ++fmoduleDeleteEndpoint++ fmoduleModifyEndpoint

  val appFModule = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
