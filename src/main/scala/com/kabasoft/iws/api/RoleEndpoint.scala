package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.UserRole
import com.kabasoft.iws.repository.Schema.{roleSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object RoleEndpoint {

  val roleCreateAPI     = Endpoint.post("role").in[UserRole].out[UserRole].outError[RepositoryError](Status.InternalServerError)
  val roleAllAPI        = Endpoint.get("role" / string("company")).out[List[UserRole]].outError[RepositoryError](Status.InternalServerError)
  val roleByIdAPI       = Endpoint.get("role" / string("id")/ string("company")).out[UserRole].outError[RepositoryError](Status.InternalServerError)
  val roleModifyAPI     = Endpoint.put(literal("role")).in[UserRole].out[UserRole].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("role" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val roleAllEndpoint        = roleAllAPI.implement(company => RoleCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val roleCreateEndpoint = roleCreateAPI.implement(role =>
    ZIO.logDebug(s"Insert user role  ${role}") *>
    RoleRepository.create(role).mapError(e => RepositoryError(e.getMessage)))
  val roleByIdEndpoint = roleByIdAPI.implement( p => RoleCache.getBy((p._1.toInt, p._2)).mapError(e => RepositoryError(e.getMessage)))
  val roleModifyEndpoint = roleModifyAPI.implement(p => ZIO.logInfo(s"Modify user role  ${p}") *>
    RoleRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    RoleRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val roleDeleteEndpoint = deleteAPI.implement(p => RoleRepository.delete(p._1.toInt, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = roleAllEndpoint ++ roleByIdEndpoint  ++ roleCreateEndpoint ++roleDeleteEndpoint++ roleModifyEndpoint

  val appRole = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
