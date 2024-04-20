package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Asset
import com.kabasoft.iws.repository.Schema.{assetSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.AssetsService
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object AssetEndpoint {

  val assetCreateAPI     = Endpoint.post("asset").in[Asset].out[Asset].outError[RepositoryError](Status.InternalServerError)
  val assetAllAPI        = Endpoint.get("asset" / int("modelid")/string("company")).out[List[Asset]].outError[RepositoryError](Status.InternalServerError)
  val assetByIdAPI       = Endpoint.get("asset" / string("id")/ string("company")).out[Asset].outError[RepositoryError](Status.InternalServerError)
  val assetModifyAPI     = Endpoint.put(literal("asset")).in[Asset].out[Asset].outError[RepositoryError](Status.InternalServerError)
  val assetGenerateAPI       = Endpoint.get("dtr" / int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("asset" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val assetAllEndpoint        = assetAllAPI.implement(p => AssetRepository.all(p).mapError(e => RepositoryError(e.getMessage)))
  val assetCreateEndpoint = assetCreateAPI.implement(asset =>
    ZIO.logDebug(s"Insert asset  ${asset}") *>
    AssetRepository.create(asset).mapError(e => RepositoryError(e.getMessage)))
  val assetByIdEndpoint = assetByIdAPI.implement( p => AssetRepository.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val assetModifyEndpoint = assetModifyAPI.implement(p => ZIO.logInfo(s"Modify asset  ${p}") *>
    AssetRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    AssetRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val assetGenerateEndpoint = assetGenerateAPI.implement( p => ZIO.logInfo(s"Generate asset1  ${p}") *>
       AssetsService.generate(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
  val assetDeleteEndpoint = deleteAPI.implement(p => AssetRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val appAsset = assetAllEndpoint ++ assetByIdEndpoint  ++ assetCreateEndpoint ++assetDeleteEndpoint++
                 assetModifyEndpoint++ assetGenerateEndpoint

}
