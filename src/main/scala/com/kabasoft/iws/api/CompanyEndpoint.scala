package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Company}
import com.kabasoft.iws.repository.CompanyRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, companySchema, repositoryErrorSchema}
import zio.schema.Schema
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.{path, int, string}
import zio.http.codec.*
import zio.http.endpoint.Endpoint
object CompanyEndpoint:
  val modelidDoc = "The modelId for identifying the typ of company "
  val idDoc = "The unique Id for identifying the store"
  val mCreateAPIFoc="Create a new company"
  val mAllAPIDoc = "Get a company by modelId "
  val mByIdAPIDoc = "Get company by Id and modelId"
  val mModifyAPIDoc = "Modify a company"
  val mDeleteAPIDoc = "Delete a company"

  private val mCreate = Endpoint(RoutePattern.POST / "comp")
    .in[Company]
    .header(HeaderCodec.authorization)
    .out[Company]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "comp" / int("modelid") ?? Doc.p(modelidDoc) 
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Company]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "comp" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Company] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "comp").header(HeaderCodec.authorization)
    .in[Company]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Company] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "comp" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createCompanyRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert company  ${m}") 
        *> CompanyRepository.create(m)
        *> CompanyRepository.getById(m.id, m.modelid)

  val companyAllRoute =
    mAll.implement : (modelid, h)=>
      ZIO.logInfo(s"get all company  ${modelid}") *>
        CompanyRepository.all(modelid)

  val companyByIdRoute =
    mById.implement: (id, modelid, h)=>
      ZIO.logInfo (s"Get company  id: ${id} modelid: ${modelid} ") *>
        CompanyRepository.getById((id, modelid))

  val modifyCompanyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify company  ${m}") *>
        CompanyRepository.modify (m) *>
        CompanyRepository.getById ((m.id, m.modelid) )

  val deleteStoreRoute =
    mDelete.implement: (id, modelid, _)  =>
      CompanyRepository.delete((id, modelid))


  val companyRoutes = Routes(createCompanyRoute, companyAllRoute, companyByIdRoute, modifyCompanyRoute, deleteStoreRoute) @@ Middleware.debug


