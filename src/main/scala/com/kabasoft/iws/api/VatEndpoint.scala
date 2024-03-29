package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{vatSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.Vat
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.Status

object VatEndpoint {

  val vatCreateAPI                                                       = Endpoint.post("vat").in[Vat].out[Vat].outError[RepositoryError](Status.InternalServerError)
  val vatAllAPI                                                          = Endpoint.get("vat"/ int("modelid")/string("company")).out[List[Vat]].outError[RepositoryError](Status.InternalServerError)
  val vatByIdAPI                                                         = Endpoint.get("vat" / string("id")/ string("company")).out[Vat].outError[RepositoryError](Status.InternalServerError)
  val vatModifyAPI     = Endpoint.put("vat").in[Vat].out[Vat].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI                                                  = Endpoint.delete("vat" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val vatCreateEndpoint      = vatCreateAPI.implement(vat => VatRepository.create(vat).mapError(e => RepositoryError(e.getMessage)))
  val vatAllEndpoint         = vatAllAPI.implement(p => VatCache.all(p).mapError(e => RepositoryError(e.getMessage)))
  val vatByIdEndpoint        = vatByIdAPI.implement(p => VatCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val vatModifyEndpoint = vatModifyAPI.implement(p => ZIO.logInfo(s"Modify vat  ${p}") *>
    VatRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    VatRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val vatDeleteEndpoint = deleteAPI.implement(p => VatRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routesVat = vatAllEndpoint ++ vatByIdEndpoint ++ vatCreateEndpoint ++vatDeleteEndpoint++vatModifyEndpoint

  val appVat = routesVat//.toApp@@ bearerAuth(jwtDecode(_).isDefined) ++ vatCreateEndpoint

}
