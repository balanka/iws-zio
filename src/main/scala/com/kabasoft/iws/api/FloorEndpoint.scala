package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Floor}
import com.kabasoft.iws.repository.FloorRepository
import com.kabasoft.iws.repository.Schema.{floorSchema, authenticationErrorSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object FloorEndpoint:
  val modelidDoc = "The modelId for identifying the typ of floor (i.e. cost center)"
  val idDoc = "The unique Id for identifying the  floor"
  val mCreateAPIFoc="Create a new floor"
  val mAllAPIDoc = "Get a floor by modelId and company"
  val companyDoc = "The company whom the floor belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get floor by Id and modelId"
  val mModifyAPIDoc = "Modify a floor"
  val mDeleteAPIDoc = "Delete a  floor"

  private val mCreate = Endpoint(RoutePattern.POST / "apt")
    .in[Floor]
    .header(HeaderCodec.authorization)
    .out[Floor]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "floor" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Floor]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "floor" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Floor] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "floor").header(HeaderCodec.authorization)
    .in[Floor]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Floor] ?? Doc.p(mModifyAPIDoc)
  
  private val mDelete = Endpoint(RoutePattern.DELETE / "floor" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val masterfileCreateRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert floor  ${m}")
        *> FloorRepository.create(m)
        *> FloorRepository.getById(m.id, m.modelid, m.company)

  val masterfileAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"get all floor with modelId ${p._2}   ${p}") *>
        FloorRepository.all((p._1, p._2))

  val masterfileByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify floor  ${p}") *>
        FloorRepository.getById(p._1, p._2, p._3)

  val masterfileModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify floor  ${m}") *>
        FloorRepository.modify (m) *>
        FloorRepository.getById ((m.id, m.modelid, m.company) )

  val masterfileDeleteRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      FloorRepository.delete((id, modelid, company))

  val floorRoutes = Routes(masterfileCreateRoute, masterfileAllRoute, masterfileByIdRoute, masterfileModifyRoute
    , masterfileDeleteRoute) @@ Middleware.debug