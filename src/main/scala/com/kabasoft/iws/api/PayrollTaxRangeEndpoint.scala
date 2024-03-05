package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.PayrollTaxRange
import com.kabasoft.iws.repository.Schema.{payrollTaxRangeSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint

object PayrollTaxRangeEndpoint {

  val mCreateAPI     = Endpoint.post("payrollTax").in[PayrollTaxRange].out[PayrollTaxRange].outError[RepositoryError](Status.InternalServerError)
  val mAllAPI        = Endpoint.get("payrollTax" /int("modelid")/ string("company")).out[List[PayrollTaxRange]].outError[RepositoryError](Status.InternalServerError)
  val mByIdAPI       = Endpoint.get("payrollTax" / string("id")/ int("modelid")/string("company")).out[PayrollTaxRange].outError[RepositoryError](Status.InternalServerError)
  val mModifyAPI     = Endpoint.put(literal("payrollTax")).in[PayrollTaxRange].out[PayrollTaxRange].outError[RepositoryError](Status.InternalServerError)
  private val mDeleteAPI = Endpoint.delete("payrollTax" / string("id")/int("modelid")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val mAllEndpoint        = mAllAPI.implement(p => ZIO.logInfo(s"Insert payroll tax range  ${p}") *>
    PayrollTaxRangeCache.all(p).mapError(e => RepositoryError(e.getMessage)))
  val mCreateEndpoint = mCreateAPI.implement(m =>
    ZIO.logInfo(s"Insert payroll tax range  ${m}") *>
      PayrollTaxRangeRepository.create(m).mapError(e => RepositoryError(e.getMessage)))
  val mByIdEndpoint = mByIdAPI.implement( p => PayrollTaxRangeCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val mModifyEndpoint = mModifyAPI.implement(m => ZIO.logInfo(s"Modify payroll tax range  ${m}") *>
    PayrollTaxRangeRepository.modify(m).mapError(e => RepositoryError(e.getMessage)) *>
    PayrollTaxRangeRepository.getBy((m.id, m.modelid, m.company)).mapError(e => RepositoryError(e.getMessage)))
  val mDeleteEndpoint = mDeleteAPI.implement(p => PayrollTaxRangeRepository.delete(p._1, p._2, p._3).mapError(e => RepositoryError(e.getMessage)))

  val appPayrollTaxRange = mAllEndpoint ++ mByIdEndpoint  ++ mCreateEndpoint ++mDeleteEndpoint++ mModifyEndpoint


}
