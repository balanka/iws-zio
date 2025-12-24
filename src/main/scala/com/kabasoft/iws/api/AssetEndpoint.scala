package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.{AuthenticationError, RepositoryError}
import com.kabasoft.iws.domain.{AppError, Asset}
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, assetSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository.AssetRepository
import com.kabasoft.iws.service.AssetsService
import zio.schema.Schema
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.{path, int, string}
import zio.http.codec.*
import zio.http.endpoint.Endpoint

object AssetEndpoint:
  val modelidDoc = "The modelId for identifying the typ of asset (i.e. cost center)"
  val idDoc = "The unique Id for identifying the asset"
  val mCreateAPIFoc = "Create a new asset"
  val mAllAPIDoc = "Get an asset by modelId and company"
  val companyDoc = "The company whom the asset belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get asset by Id and modelId"
  val mModifyAPIDoc = "Modify an asset"
  val mDeleteAPIDoc = "Delete an asset"
  val mGenerateAPIDoc = "Generate asset depreciation plan"

  private val mCreate = Endpoint(RoutePattern.POST / "asset")
    .in[Asset]
    .header(HeaderCodec.authorization)
    .out[Asset]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "asset" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Asset]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "asset" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Asset] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "asset").header(HeaderCodec.authorization)
    .in[Asset]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Asset] ?? Doc.p(mModifyAPIDoc)
  
  private val assetGenerateDepr   = Endpoint(RoutePattern.GET /"dtr" / int("modelid")?? Doc.p(modelidDoc)
       /string("company")?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mGenerateAPIDoc)
  
  private val mDelete = Endpoint(RoutePattern.DELETE / "asset" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert asset  ${m}")
        *>  AssetRepository.create(m)
        *>  AssetRepository.getById(m.id, m.modelid, m.company)

  val mAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Get all asset  ${p}") *>
        AssetRepository.all((p._1, p._2))

  val mByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Get asset by id  ${p}") *>
        AssetRepository.getById(p._1, p._2, p._3)

  val mModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo(s"Modify asset  ${m}") *>
        AssetRepository.modify(m) *>
        AssetRepository.getById((m.id, m.modelid, m.company))
      
  val mGenerateDeprRoute =
    assetGenerateDepr.implement: (modelid, company, _) =>
      ZIO.logInfo(s"Generate asset depreciation plan modelid = ${modelid} comapany = $company") *>
        AssetsService.generate(modelid, company)
      
  val mDeleteRoute =
    mDelete.implement: (id, modelid, company, _) =>
      AssetRepository.delete((id, modelid, company))


  val assetRoutes = Routes(createRoute, mAllRoute, mByIdRoute, mModifyRoute, mDeleteRoute, mGenerateDeprRoute) @@ Middleware.debug

