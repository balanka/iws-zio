package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, ImportFile}
import com.kabasoft.iws.repository.ImportFileRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, importFileSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object ImportFileEndpoint:
  val modelidDoc = "The modelId for identifying the typ of store "
  val idDoc = "The unique Id for identifying the store"
  val mCreateAPIFoc="Create a new store"
  val mAllAPIDoc = "Get a store by modelId and company"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get store by Id and modelId"
  val mModifyAPIDoc = "Modify a store"
  val mDeleteAPIDoc = "Delete a store"

  private val mCreate = Endpoint(RoutePattern.POST / "store")
    .in[ImportFile]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "impfile" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[ImportFile]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "impfile" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[ImportFile] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "impfile").header(HeaderCodec.authorization)
    .in[ImportFile]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[ImportFile] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "impfile" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createImportFileRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert store  ${m}") *>
        ImportFileRepository.create(m)

  val importFileAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"Insert store  ${p}") *>
        ImportFileRepository.all((p._1, p._2))

  val importFileByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify store  ${p}") *>
        ImportFileRepository.getById(p._1, p._2, p._3)

  val modifyImportFileRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo (s"Modify store  ${m}") *>
        ImportFileRepository.modify (m) *>
        ImportFileRepository.getById ((m.id, m.modelid, m.company) )

  val deleteImportFileRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      ImportFileRepository.delete((id, modelid, company))
  
  val importFileRoutes = Routes(createImportFileRoute, importFileAllRoute, importFileByIdRoute, modifyImportFileRoute, deleteImportFileRoute) @@ Middleware.debug



