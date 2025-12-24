package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Module}
import com.kabasoft.iws.repository.ModuleRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, moduleSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object ModuleEndpoint:
  val modelidDoc = "The modelId for identifying the typ of module "
  val idDoc = "The unique Id for identifying the  module"
  val mCreateAPIFoc = "Create a new module"
  val mAllAPIDoc = "Get a module by modelId and company"
  val companyDoc = "The company whom the module belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get module by Id and modelId"
  val mModifyAPIDoc = "Modify a module"
  val mDeleteAPIDoc = "Delete a  module"

  private val mCreate = Endpoint(RoutePattern.POST / "module")
    .in[Module]
    .header(HeaderCodec.authorization)
    .out[Module]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "module" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Module]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "module" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Module] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "module").header(HeaderCodec.authorization)
    .in[Module]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Module] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "module" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert module  ${m}") 
        *> ModuleRepository.create(m)
        *> ModuleRepository.getById(m.id, m.modelid, m.company)

  val mAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"get aLl module  ${p}") *>
        ModuleRepository.all((p._1, p._2))

  val mByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify module  ${p}") *>
        ModuleRepository.getById(p._1, p._2, p._3)

  val mModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo(s"Modify module  ${m}") *>
        ModuleRepository.modify(m) *>
        ModuleRepository.getById((m.id, m.modelid, m.company))

  val mDeleteRoute =
      mDelete.implement: (id, modelid, company, _) =>
       ModuleRepository.delete((id, modelid, company))
  
  val moduleRoutes = Routes(createRoute, mAllRoute, mByIdRoute, mModifyRoute, mDeleteRoute) @@ Middleware.debug
