package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Customer}
import com.kabasoft.iws.repository.CustomerRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, customerSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object CustomerEndpoint:
  val modelidDoc = "The modelId for identifying the typ of customer "
  val idDoc = "The unique Id for identifying the customer"
  val mCreateAPIFoc = "Create a new customer"
  val mAllAPIDoc = "Get a customer by modelId and company"
  val companyDoc = "The company whom the customer belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get customer by Id and modelId"
  val mModifyAPIDoc = "Modify a customer"
  val mDeleteAPIDoc = "Delete a  customer"

  private val mCreate = Endpoint(RoutePattern.POST / "cust")
    .in[Customer]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "cust" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Customer]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "cust" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Customer] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "cust").header(HeaderCodec.authorization)
    .in[Customer]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Customer] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "cust" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createCustomerRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert customer  ${m}") *>
        CustomerRepository.create(m, true)

  val customerAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert customer  ${p}") *>
        CustomerRepository.all((p._1, p._2))

  val customerByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify supplier  ${p}") *>
        CustomerRepository.getById(p._1, p._2, p._3)

  val modifyCustomerRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify customer  ${m}") *>
        CustomerRepository.modify(m) *>
        CustomerRepository.getById((m.id, m.modelid, m.company))

  val deleteCustomerRoute =
    mDelete.implement: (id, modelid, company, _) =>
      CustomerRepository.delete((id, modelid, company))


  val customerRoutes = Routes(createCustomerRoute, customerAllRoute, customerByIdRoute, modifyCustomerRoute, deleteCustomerRoute) @@ Middleware.debug


