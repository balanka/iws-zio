package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.service.EmployeeService
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.EndpointMiddleware.None
import zio.http.endpoint.{Endpoint, Routes}

object PayrollEndpoint {

  val createPayrollAPI       = Endpoint.get("ptr" /  string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val createPayrollEndpoint = createPayrollAPI.implement(p =>
    ZIO.logInfo(s"Create payroll transaction from salary item ${p}") *>
      EmployeeService.generate(p).mapError(e => RepositoryError(e.getMessage)))

  val appPayroll: Routes[EmployeeService, RepositoryError, None] = createPayrollEndpoint

}
