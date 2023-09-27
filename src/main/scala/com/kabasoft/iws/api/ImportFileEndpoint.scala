package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.ImportFile
import com.kabasoft.iws.repository.Schema.{importFileSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object ImportFileEndpoint {

  val importFileCreateAPI     = Endpoint.post("impfile").in[ImportFile].out[ImportFile].outError[RepositoryError](Status.InternalServerError)
  val importFileAllAPI        = Endpoint.get("impfile" / string("company")).out[List[ImportFile]].outError[RepositoryError](Status.InternalServerError)
  val importFileByIdAPI       = Endpoint.get("impfile" / string("id")/ string("company")).out[ImportFile].outError[RepositoryError](Status.InternalServerError)
  val importFileModifyAPI     = Endpoint.put(literal("impfile")).in[ImportFile].out[ImportFile].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("impfile" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val importFileAllEndpoint        = importFileAllAPI.implement(company => ImportFileCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val importFileCreateEndpoint = importFileCreateAPI.implement(bank =>
    ZIO.logDebug(s"Insert importFile  ${bank}") *>
      ImportFileRepository.create(bank).mapError(e => RepositoryError(e.getMessage)))
  val importFileByIdEndpoint = importFileByIdAPI.implement( p => ImportFileCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val bankModifyEndpoint = importFileModifyAPI.implement(p => ZIO.logInfo(s"Modify importFile  ${p}") *>
    ImportFileRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    ImportFileRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val importFileDeleteEndpoint = deleteAPI.implement(p => ImportFileRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val appImportFile = importFileAllEndpoint ++ importFileByIdEndpoint  ++ importFileCreateEndpoint ++importFileDeleteEndpoint++ bankModifyEndpoint

}
