package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain.{AppError, RealEstate}
import com.kabasoft.iws.repository.RealEstateRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, realEstateSchema}
import zio._
import zio.http._
import zio.http.codec._
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object RealEstateEndpoint:
  val modelidDoc = "The modelId for identifying the typ of real estate (i.e. real estate)"
  val idDoc = "The unique Id for identifying the  real estate"
  val mCreateAPIFoc="Create a new real estate"
  val mAllAPIDoc = "Get a room by modelId and company"
  val companyDoc = "The company whom the room belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get room by Id and modelId"
  val mModifyAPIDoc = "Modify a real estate"
  val mDeleteAPIDoc = "Delete a  real estate"

  private val mCreate = Endpoint(RoutePattern.POST / "real")
    .in[RealEstate]
    .header(HeaderCodec.authorization)
    .out[RealEstate]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "real" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  )//.header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[RealEstate]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "real" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[RealEstate] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "real").header(HeaderCodec.authorization)
    .in[RealEstate]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[RealEstate] ?? Doc.p(mModifyAPIDoc)
  
  private val mDelete = Endpoint(RoutePattern.DELETE / "real" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val masterfileCreateRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert realEstate  ${m}")
        *> RealEstateRepository.create(m)
        *> RealEstateRepository.getById(m.id, m.modelid, m.company)

  val masterfileAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"get all realEstate with modelId ${p._2}   ${p}") *>
        RealEstateRepository.all((p._1, p._2))

  val masterfileByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify realEstate  ${p}") *>
        RealEstateRepository.getById(p._1, p._2, p._3)

  val masterfileModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify realEstate  ${m}") *>
        RealEstateRepository.modify (m) *>
        RealEstateRepository.getById ((m.id, m.modelid, m.company) )

  val masterfileDeleteRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      RealEstateRepository.delete((id, modelid, company))

  val masterfileRoutes = Routes(masterfileCreateRoute, masterfileAllRoute, masterfileByIdRoute, masterfileModifyRoute
    , masterfileDeleteRoute) @@ Middleware.debug