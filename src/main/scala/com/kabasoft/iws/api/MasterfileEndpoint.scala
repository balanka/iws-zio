package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.repository.Schema.{masterfileSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object MasterfileEndpoint {

  val mCreateAPI     = Endpoint.post("mf").in[Masterfile].out[Masterfile].outError[RepositoryError](Status.InternalServerError)
  val mAllAPI        = Endpoint.get("mf" /int("modelid")/ string("company")).out[List[Masterfile]].outError[RepositoryError](Status.InternalServerError)
  val mByIdAPI       = Endpoint.get("mf" / string("id")/ int("modelid")/string("company")).out[Masterfile].outError[RepositoryError](Status.InternalServerError)
  val mModifyAPI     = Endpoint.put(literal("mf")).in[Masterfile].out[Masterfile].outError[RepositoryError](Status.InternalServerError)
  private val mDeleteAPI = Endpoint.delete("mf" / string("id")/int("modelid")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val mAllEndpoint        = mAllAPI.implement(p => MasterfileRepository.all(p).mapError(e => RepositoryError(e.getMessage)))
  val mCreateEndpoint = mCreateAPI.implement(m =>
    ZIO.logInfo(s"Insert masterfile  ${m}") *>
      MasterfileRepository.create(m).mapError(e => RepositoryError(e.getMessage)))
  val mByIdEndpoint = mByIdAPI.implement( p => MasterfileRepository.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val mModifyEndpoint = mModifyAPI.implement(m => ZIO.logInfo(s"Modify masterfile  ${m}") *>
    MasterfileRepository.modify(m).mapError(e => RepositoryError(e.getMessage)) *>
    MasterfileRepository.getBy((m.id, m.modelid, m.company)).mapError(e => RepositoryError(e.getMessage)))
  val mDeleteEndpoint = mDeleteAPI.implement(p => MasterfileRepository.delete(p._1, p._2, p._3).mapError(e => RepositoryError(e.getMessage)))

  val appMasterfile = mAllEndpoint ++ mByIdEndpoint  ++ mCreateEndpoint ++mDeleteEndpoint++ mModifyEndpoint


}
