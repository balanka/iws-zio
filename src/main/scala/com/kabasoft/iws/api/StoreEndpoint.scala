package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Store
import com.kabasoft.iws.repository.Schema.{storeSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object StoreEndpoint {

  val storeCreateAPI     = Endpoint.post("store").in[Store].out[Store].outError[RepositoryError](Status.InternalServerError)
  val storeAllAPI        = Endpoint.get("store" / int("modelid")/string("company")).out[List[Store]].outError[RepositoryError](Status.InternalServerError)
  val storeByIdAPI       = Endpoint.get("bank" / string("id")/ string("company")).out[Store].outError[RepositoryError](Status.InternalServerError)
  val storeModifyAPI     = Endpoint.put(literal("store")).in[Store].out[Store].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("store" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val storeAllEndpoint        = storeAllAPI.implement(p => StoreCache.all(p).mapError(e => RepositoryError(e.getMessage)))
  val storeCreateEndpoint = storeCreateAPI.implement(store =>
    ZIO.logDebug(s"Insert store  ${store}") *>
      StoreRepository.create(store).mapError(e => RepositoryError(e.getMessage)))
  val storeByIdEndpoint = storeByIdAPI.implement( p => StoreCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val storeModifyEndpoint = storeModifyAPI.implement(p => ZIO.logInfo(s"Modify store  ${p}") *>
    StoreRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    StoreRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val storeDeleteEndpoint = deleteAPI.implement(p => StoreRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = storeAllEndpoint ++ storeByIdEndpoint  ++ storeCreateEndpoint ++storeDeleteEndpoint++ storeModifyEndpoint

  val appStore = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
