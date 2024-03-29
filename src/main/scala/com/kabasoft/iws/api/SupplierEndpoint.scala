package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{supplierschema, repositoryErrorSchema}
import com.kabasoft.iws.domain.Supplier
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec.{string, _}
import zio.http.endpoint.Endpoint
import zio.http.Status


object SupplierEndpoint {

  val supCreateAPI      = Endpoint.post("sup").in[Supplier].out[Supplier].outError[RepositoryError](Status.InternalServerError)
  val supAllAPI         = Endpoint.get("sup"/int("modelid")/ string("company")).out[List[Supplier]].outError[RepositoryError](Status.InternalServerError)
  val supByIdAPI        = Endpoint.get("sup" / string("id")/ string("company")).out[Supplier].outError[RepositoryError](Status.InternalServerError)
  val supModifyAPI     = Endpoint.put(literal("sup")).in[Supplier].out[Supplier].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("sup" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val supCreateEndpoint      = supCreateAPI.implement(sup => ZIO.logInfo(s"Create supplier  ${sup}") *>
    SupplierRepository.create(sup).mapError(e => RepositoryError(e.getMessage)))
  val supAllEndpoint         = supAllAPI.implement(p => SupplierCache.all(p).mapError(e => RepositoryError(e.getMessage)))
  val supByIdEndpoint        = supByIdAPI.implement( p => SupplierCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val supModifyEndpoint = supModifyAPI.implement(p => ZIO.logInfo(s"Modify supplier  ${p}") *>
    SupplierRepository.update(p).mapError(e => RepositoryError(e.getMessage)))
    //SupplierRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val supDeleteEndpoint = deleteAPI.implement(p => SupplierRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

   val routesSup = supAllEndpoint ++ supByIdEndpoint  ++supDeleteEndpoint++supModifyEndpoint++ supCreateEndpoint

  val appSup = routesSup//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ supCreateEndpoint
}
