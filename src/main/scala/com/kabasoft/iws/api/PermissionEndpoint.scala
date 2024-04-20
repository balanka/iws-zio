package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Permission
import com.kabasoft.iws.repository.Schema.{permissionSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object PermissionEndpoint {

  val permCreateAPI     = Endpoint.post("perm").in[Permission].out[Permission].outError[RepositoryError](Status.InternalServerError)
  val permAllAPI        = Endpoint.get("perm" / string("company")).out[List[Permission]].outError[RepositoryError](Status.InternalServerError)
  val permByIdAPI       = Endpoint.get("perm" / string("id")/ string("company")).out[Permission].outError[RepositoryError](Status.InternalServerError)
  val permModifyAPI     = Endpoint.put(literal("perm")).in[Permission].out[Permission].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("perm" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val permAllEndpoint        = permAllAPI.implement(company => ZIO.logInfo(s"get all permission for   ${company}") *>
    PermissionRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  val permCreateEndpoint = permCreateAPI.implement(perm =>
    ZIO.logInfo(s"Insert perm  ${perm}") *>
      PermissionRepository.create(perm).mapError(e => RepositoryError(e.getMessage)))
  val permByIdEndpoint = permByIdAPI.implement( p => PermissionRepository.getBy((p._1.toInt, p._2)).mapError(e => RepositoryError(e.getMessage)))
  val permModifyEndpoint = permModifyAPI.implement(p => ZIO.logInfo(s"Modify perm  ${p}") *>
    PermissionRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    PermissionRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val permDeleteEndpoint = deleteAPI.implement(p => PermissionRepository.delete(p._1.toInt, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = permAllEndpoint ++ permByIdEndpoint  ++ permCreateEndpoint ++permDeleteEndpoint++ permModifyEndpoint

  val appPerm = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
