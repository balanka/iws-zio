package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, PayrollTaxRange}
import com.kabasoft.iws.repository.PayrollTaxRangeRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, payrollTaxRangeSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object PayrollTaxRangeEndpoint:
  val modelidDoc = "The modelId for identifying the typ of payroll tax  "
  val idDoc = "The unique Id for identifying the payroll tax "
  val mCreateAPIFoc = "Create a new payroll tax "
  val mAllAPIDoc = "Get a payroll tax  by modelId and company"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get payroll tax  by Id and modelId"
  val mModifyAPIDoc = "Modify a payroll tax "
  val mDeleteAPIDoc = "Delete a payroll tax "

  private val mCreate = Endpoint(RoutePattern.POST / "payrollTax")
    .in[PayrollTaxRange]
    .header(HeaderCodec.authorization)
    .out[PayrollTaxRange]
    .outErrors[AppError](
      HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(
    RoutePattern.GET / "payrollTax" / int("modelid") ?? Doc.p(
      modelidDoc
    ) / string("company") ??
      Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](
      HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )
    .out[List[PayrollTaxRange]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(
    RoutePattern.GET / "payrollTax" / string("id") ?? Doc.p(idDoc) / int(
      "modelid"
    ) ?? Doc.p(modelidDoc)
      / string("company") ?? Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](
      HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )
    .out[PayrollTaxRange] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "payrollTax")
    .header(HeaderCodec.authorization)
    .in[PayrollTaxRange]
    .outErrors[AppError](
      HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )
    .out[PayrollTaxRange] ?? Doc.p(mModifyAPIDoc)

  private val mDelete = Endpoint(
    RoutePattern.DELETE / "payrollTax" / string("id") ?? Doc.p(
      modelidDoc
    ) / int("modelid") ?? Doc.p(modelidDoc)
      / string("company") ?? Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](
      HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )
    .out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createPayrollTaxRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert payroll tax   ${m}") 
        *> PayrollTaxRangeRepository.create(m)
        *> PayrollTaxRangeRepository.getById(m.id, m.modelid, m.company)

  val payrollTaxAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert payroll tax   ${p}") *>
        PayrollTaxRangeRepository.all((p._1, p._2))

  val payrollTaxByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify payroll tax   ${p}") *>
        PayrollTaxRangeRepository.getById(p._1, p._2, p._3)

  val modifyPayrollTaxRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify payroll tax  ${m}") *>
        PayrollTaxRangeRepository.modify(m) *>
        PayrollTaxRangeRepository.getById((m.id, m.modelid, m.company))

  val deletePayrollTaxRoute =
    mDelete.implement: (id, modelid, company, _) =>
      PayrollTaxRangeRepository.delete((id, modelid, company))

  val payrollTaxRoutes = Routes(
    createPayrollTaxRoute,
    payrollTaxAllRoute,
    payrollTaxByIdRoute,
    modifyPayrollTaxRoute,
    deletePayrollTaxRoute
  ) @@ Middleware.debug
