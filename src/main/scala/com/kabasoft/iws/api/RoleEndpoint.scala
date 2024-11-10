package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Role}
import com.kabasoft.iws.repository.RoleRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, roleSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object RoleEndpoint:
  val modelidDoc = "The modelId for identifying the typ of role "
  val idDoc = "The unique Id for identifying the role"
  val mCreateAPIFoc = "Create a new role"
  val mAllAPIDoc = "Get a role by modelId and company"
  val companyDoc = "The company whom the role belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get role by Id and modelId"
  val mModifyAPIDoc = "Modify a role"
  val mDeleteAPIDoc = "Delete a role"

  private val mCreate = Endpoint(RoutePattern.POST / "role")
    .in[Role]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "role" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Role]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "role" / int("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Role] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "role").header(HeaderCodec.authorization)
    .in[Role]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Role] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "role" / int("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val creatRoleRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert role  ${m}") *>
        RoleRepository.create(m, true)

  val roleAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert role  ${p}") *>
        RoleRepository.all((p._1, p._2))

  val roleByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify role  ${p}") *>
        RoleRepository.getById(p._1, p._2, p._3)

  val modifyRoleRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify role  ${m}") *>
        RoleRepository.modify(m) *>
        RoleRepository.getById((m.id, m.modelid, m.company))

  val deleteRoleRoute =
    mDelete.implement: (id, modelid, company, _) =>
      RoleRepository.delete((id, modelid, company))
  val roleRoutes = Routes(creatRoleRoute, roleAllRoute, roleByIdRoute, modifyRoleRoute, deleteRoleRoute) @@ Middleware.debug