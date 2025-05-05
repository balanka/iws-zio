package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.{AuthenticationError, RepositoryError}
import com.kabasoft.iws.domain.{AppError, SalaryItem}
import com.kabasoft.iws.repository.SalaryItemRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, salaryItemSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object SalaryItemEndpoint:
  val modelidDoc = "The modelId for identifying the typ of salary item "
  val idDoc = "The unique Id for identifying the salary item"
  val mCreateAPIFoc = "Create a new salary item"
  val mAllAPIDoc = "Get a salary item by modelId and company"
  val companyDoc = "The company whom the salary item belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get salary item by Id and modelId"
  val mModifyAPIDoc = "Modify a salary item"
  val mDeleteAPIDoc = "Delete a salary item"

  private val mCreate = Endpoint(RoutePattern.POST / "s_item")
    .in[SalaryItem]
    .header(HeaderCodec.authorization)
    .out[SalaryItem]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "s_item" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[SalaryItem]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "s_item" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[SalaryItem] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "s_item").header(HeaderCodec.authorization)
    .in[SalaryItem]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[SalaryItem] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "s_item" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createStoreRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert salary item  ${m}") 
        *> SalaryItemRepository.create(m)
        *> SalaryItemRepository.getById(m.id, m.modelid, m.company)
      

  val storeAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert salary item  ${p}") *>
        SalaryItemRepository.all((p._1, p._2))

  val storeByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify salary item  ${p}") *>
        SalaryItemRepository.getById(p._1, p._2, p._3)

  val modifyStoreRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify salary item  ${m}") *>
        SalaryItemRepository.modify(m) *>
        SalaryItemRepository.getById((m.id, m.modelid, m.company))

  val deleteStoreRoute =
    mDelete.implement: (id, modelid, company, _) =>
      SalaryItemRepository.delete((id, modelid, company))
  val salaryItemRoutes = Routes(createStoreRoute, storeAllRoute, storeByIdRoute, modifyStoreRoute, deleteStoreRoute) @@ Middleware.debug