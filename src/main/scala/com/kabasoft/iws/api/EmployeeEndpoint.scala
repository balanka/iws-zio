package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Employee}
import com.kabasoft.iws.repository.EmployeeRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, employeeSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object EmployeeEndpoint:
  val modelidDoc = "The modelId for identifying the typ of employee "
  val idDoc = "The unique Id for identifying the employee"
  val mCreateAPIFoc="Create a new employee"
  val mAllAPIDoc = "Get an employee by modelId and company"
  val companyDoc = "The company whom the supplier belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get employee by Id and modelId"
  val mModifyAPIDoc = "Modify an employee"
  val mDeleteAPIDoc = "Delete an employee"

  private val mCreate = Endpoint(RoutePattern.POST / "emp")
    .in[Employee]
    .header(HeaderCodec.authorization)
    .out[Employee]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "emp" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Employee]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "emp" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Employee] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "emp").header(HeaderCodec.authorization)
    .in[Employee]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Employee] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "emp" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createEmployeeRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert employee  ${m}") 
        *> EmployeeRepository.create(m)
        *> EmployeeRepository.getById(m.id, m.modelid, m.company)

  val employeeAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"get all employee  ${p}") *>
        EmployeeRepository.all((p._1, p._2))

  val employeeByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify employee  ${p}") *>
        EmployeeRepository.getById(p._1, p._2, p._3)

  val modifyEmployeeRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify employee  ${m}") *>
        EmployeeRepository.modify (m) *>
        EmployeeRepository.getById ((m.id, m.modelid, m.company) )

  val deleteEmployeeRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      EmployeeRepository.delete((id, modelid, company))
  val employeeRoutes = Routes(createEmployeeRoute, employeeAllRoute, employeeByIdRoute, modifyEmployeeRoute, deleteEmployeeRoute) @@ Middleware.debug


