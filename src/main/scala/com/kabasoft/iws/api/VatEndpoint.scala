package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.vatSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Vat
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status

object VatEndpoint {

  val vatCreateAPI                                                       = Endpoint.post("vat").in[Vat].out[Int].outError[RepositoryError](Status.InternalServerError)
  val vatAllAPI                                                          = Endpoint.get("vat"/ string("company")).out[List[Vat]].outError[RepositoryError](Status.InternalServerError)
  val vatByIdAPI                                                         = Endpoint.get("vat" / string("id")/ string("company")).out[Vat].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI                                                  = Endpoint.delete("vat" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val vatCreateEndpoint      = vatCreateAPI.implement(vat => VatRepository.create(List(vat)).mapError(e => RepositoryError(e.getMessage)))
  val vatAllEndpoint         = vatAllAPI.implement(company => VatRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  val vatByIdEndpoint        = vatByIdAPI.implement(p => VatRepository.getBy(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
   val vatDeleteEndpoint = deleteAPI.implement(p => VatRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

   val routesVat = vatAllEndpoint ++ vatByIdEndpoint ++ vatCreateEndpoint ++vatDeleteEndpoint

  val appVat = routesVat//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ vatCreateEndpoint

}
