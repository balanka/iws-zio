package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Employee
import com.kabasoft.iws.repository.Schema.{employeeSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.Endpoint
import zio.schema.DeriveSchema.gen

object EmployeeEndpoint {

  val empCreateAPI      = Endpoint.post("emp").in[Employee].out[Employee].outError[RepositoryError](Status.InternalServerError)
  val empAllAPI         = Endpoint.get("emp"/  int("modelid")/string("company")).out[List[Employee]].outError[RepositoryError](Status.InternalServerError)
  val empByIdAPI        = Endpoint.get("emp" / string("id")/ string("company")).out[Employee].outError[RepositoryError](Status.InternalServerError)
  val empModifyAPI     = Endpoint.put("emp").in[Employee].out[Employee].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI  = Endpoint.delete("emp" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val empCreateEndpoint     = empCreateAPI.implement (emp=> ZIO.logInfo(s"Create Employee  ${emp}") *>EmployeeRepository.create(emp).mapError(e => RepositoryError(e.getMessage)))
  val empAllEndpoint        = empAllAPI.implement (p=> EmployeeRepository.all(p).mapError(e => RepositoryError(e.getMessage)))
  val empByIdEndpoint       = empByIdAPI.implement(p => EmployeeRepository.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val ccModifyEndpoint = empModifyAPI.implement(p => ZIO.logInfo(s"Modify Employee  ${p}") *>
    EmployeeRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    EmployeeRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val empDeleteEndpoint = deleteAPI.implement(p => EmployeeRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routesEmp = empAllEndpoint ++ empByIdEndpoint ++ empCreateEndpoint ++empDeleteEndpoint ++ccModifyEndpoint

  //val appEmp= routesemp//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ empCreateEndpoint
}
