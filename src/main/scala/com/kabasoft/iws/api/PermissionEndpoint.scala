package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Permission}
import com.kabasoft.iws.repository.PermissionRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, permissionSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object PermissionEndpoint:
  val modelidDoc = "The modelId for identifying the typ of store "
  val idDoc = "The unique Id for identifying the store"
  val mCreateAPIFoc="Create a new store"
  val mAllAPIDoc = "Get a store by modelId and company"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get store by Id and modelId"
  val mModifyAPIDoc = "Modify a store"
  val mDeleteAPIDoc = "Delete a store"

  private val mCreate = Endpoint(RoutePattern.POST / "perm")
    .in[Permission]
    .header(HeaderCodec.authorization)
    .out[Permission]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "perm" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Permission]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "perm" / int("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Permission] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "perm").header(HeaderCodec.authorization)
    .in[Permission]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Permission] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "perm" / int("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createStoreRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert permissiom  ${m}") 
        *> PermissionRepository.create(m)
        *> PermissionRepository.getById(m.id, m.modelid, m.company)

  val allPermRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"Get all  permissiom  ${p}") *>
        PermissionRepository.all((p._1, p._2))

  val permByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify store  ${p}") *>
        PermissionRepository.getById(p._1, p._2, p._3)

  val modifyPermRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify store  ${m}") *>
        PermissionRepository.modify (m) *>
        PermissionRepository.getById ((m.id, m.modelid, m.company) )

  val deletePermRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      PermissionRepository.delete((id, modelid, company))
  val permissionRoutes = Routes(createStoreRoute, allPermRoute, permByIdRoute, modifyPermRoute, deletePermRoute) @@ Middleware.debug

