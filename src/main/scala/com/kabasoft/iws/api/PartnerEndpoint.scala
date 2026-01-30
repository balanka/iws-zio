package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Partner}
import com.kabasoft.iws.repository.PartnerRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, partnerSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object PartnerEndpoint:
  val modelidDoc = "The modelId for identifying the typ of partner (i.e. cost center)"
  val idDoc = "The unique Id for identifying the  partner"
  val mCreateAPIFoc="Create a new partner"
  val mAllAPIDoc = "Get a partner by modelId and company"
  val companyDoc = "The company whom the partner belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get the partner by Id and modelId"
  val mModifyAPIDoc = "Modify a partner"
  val mDeleteAPIDoc = "Delete a  partner"

  private val mCreate = Endpoint(RoutePattern.POST / "partner")
    .in[Partner]
    .header(HeaderCodec.authorization)
    .out[Partner]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "partner" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  )//.header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Partner]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "partner" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Partner] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "partner").header(HeaderCodec.authorization)
    .in[Partner]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Partner] ?? Doc.p(mModifyAPIDoc)
  
  private val mDelete = Endpoint(RoutePattern.DELETE / "partner" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val partnerCreateRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert partner  ${m}")
        *> PartnerRepository.create(m)
        *> PartnerRepository.getById(m.id, m.modelid, m.company)

  val partnerAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"get all partner with modelId ${p._2}   ${p}") *>
        PartnerRepository.all((p._1, p._2))

  val partnerByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify partner  ${p}") *>
        PartnerRepository.getById(p._1, p._2, p._3)

  val partnerModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify partner  ${m}") *>
        PartnerRepository.modify (m) *>
        PartnerRepository.getById ((m.id, m.modelid, m.company) )

  val partnerDeleteRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      PartnerRepository.delete((id, modelid, company))

  val partnerRoutes = Routes(partnerCreateRoute, partnerAllRoute, partnerByIdRoute, partnerModifyRoute
    , partnerDeleteRoute) @@ Middleware.debug