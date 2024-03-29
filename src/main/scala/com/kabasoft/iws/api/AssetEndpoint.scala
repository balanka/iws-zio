package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Asset
import com.kabasoft.iws.repository.Schema.{assetSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object AssetEndpoint {

  val assetCreateAPI     = Endpoint.post("asset").in[Asset].out[Asset].outError[RepositoryError](Status.InternalServerError)
  val assetAllAPI        = Endpoint.get("asset" / int("company")/string("company")).out[List[Asset]].outError[RepositoryError](Status.InternalServerError)
  val assetByIdAPI       = Endpoint.get("asset" / string("id")/ string("company")).out[Asset].outError[RepositoryError](Status.InternalServerError)
  val assetModifyAPI     = Endpoint.put(literal("asset")).in[Asset].out[Asset].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("asset" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val assetAllEndpoint        = assetAllAPI.implement(p => AssetCache.all(p).mapError(e => RepositoryError(e.getMessage)))
  val assetCreateEndpoint = assetCreateAPI.implement(asset =>
    ZIO.logDebug(s"Insert asset  ${asset}") *>
    AssetRepository.create(asset).mapError(e => RepositoryError(e.getMessage)))
  val bankByIdEndpoint = assetByIdAPI.implement( p => AssetCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val assetModifyEndpoint = assetModifyAPI.implement(p => ZIO.logInfo(s"Modify asset  ${p}") *>
    AssetRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    AssetRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val assetDeleteEndpoint = deleteAPI.implement(p => AssetRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = assetAllEndpoint ++ bankByIdEndpoint  ++ assetCreateEndpoint ++assetDeleteEndpoint++ assetModifyEndpoint

  val appAsset = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
