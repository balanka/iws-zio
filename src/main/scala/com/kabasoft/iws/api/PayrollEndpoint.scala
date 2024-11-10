package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema}
import com.kabasoft.iws.service.EmployeeService
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object PayrollEndpoint:
  val modelidDoc = "The modelId for identifying the typ of store "
  val accountIdDoc = "The unique Id for identifying the store"
  val periodDoc = "The  period (Format 'YYYYMM') 4 which to select the periodic account balance"
  val generatePayrollDoc = "Generate payroll transaction entries"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  
  private val generatePayroll = Endpoint(RoutePattern.GET / "ptr" /int("period") ?? Doc.p(periodDoc)/ string("company") ?? Doc.p(companyDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Int] ?? Doc.p(generatePayrollDoc)

  val generatePayrollRoute = generatePayroll.implement(p =>
    ZIO.logInfo(s"Create payroll transaction from salary item ${p}") *>
      EmployeeService.generate(p._1, p._2))
  
  val payrollRoutes = Routes(generatePayrollRoute) @@ Middleware.debug


