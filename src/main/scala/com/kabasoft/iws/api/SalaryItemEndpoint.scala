package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.SalaryItem
import com.kabasoft.iws.repository.Schema.{salaryItemSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object SalaryItemEndpoint {

  val salaryItemCreateAPI     = Endpoint.post("s_item").in[SalaryItem].out[SalaryItem].outError[RepositoryError](Status.InternalServerError)
  val salaryItemAllAPI        = Endpoint.get("s_item" / string("company")).out[List[SalaryItem]].outError[RepositoryError](Status.InternalServerError)
  val salaryItemByIdAPI       = Endpoint.get("s_item" / string("id")/ string("company")).out[SalaryItem].outError[RepositoryError](Status.InternalServerError)
  val SalaryItemModifyAPI     = Endpoint.put(literal("s_item")).in[SalaryItem].out[SalaryItem].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("s_item" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val salaryItemAllEndpoint        = salaryItemAllAPI.implement(company => SalaryItemCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val salaryItemCreateEndpoint = salaryItemCreateAPI.implement(SalaryItem =>
    ZIO.logInfo(s"Create SalaryItem  ${SalaryItem}") *>
      SalaryItemRepository.create(SalaryItem).mapError(e => RepositoryError(e.getMessage)))
  val salaryItemByIdEndpoint = salaryItemByIdAPI.implement( p => SalaryItemCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val salaryItemModifyEndpoint = SalaryItemModifyAPI.implement(p => ZIO.logInfo(s"Modify SalaryItem  ${p}") *>
    SalaryItemRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    SalaryItemRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val salaryItemDeleteEndpoint = deleteAPI.implement(p => SalaryItemRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = salaryItemAllEndpoint ++ salaryItemByIdEndpoint  ++ salaryItemCreateEndpoint ++salaryItemDeleteEndpoint++ salaryItemModifyEndpoint

  val appSalaryItem = routes

}
