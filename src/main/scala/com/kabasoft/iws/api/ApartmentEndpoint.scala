package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain.{AppError, Apartment}
import com.kabasoft.iws.repository.ApartmentRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, aparmrntSchema, repositoryErrorSchema}
import zio._
import zio.http._
import zio.http.codec._
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object ApartmentEndpoint:
  val modelidDoc = "The modelId for identifying the typ of apartment (i.e. cost center)"
  val idDoc = "The unique Id for identifying the  apartment"
  val mCreateAPIFoc="Create a new apartment"
  val mAllAPIDoc = "Get a apartment by modelId and company"
  val companyDoc = "The company whom the apartment belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get apartment by Id and modelId"
  val mModifyAPIDoc = "Modify a apartment"
  val mDeleteAPIDoc = "Delete a  apartment"

  private val mCreate = Endpoint(RoutePattern.POST / "apt")
    .in[Apartment]
    .header(HeaderCodec.authorization)
    .out[Apartment]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "apt" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  )//.header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Apartment]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "apt" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Apartment] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "apt").header(HeaderCodec.authorization)
    .in[Apartment]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Apartment] ?? Doc.p(mModifyAPIDoc)
  
  private val mDelete = Endpoint(RoutePattern.DELETE / "apt" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val masterfileCreateRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert apartment  ${m}")
        *> ApartmentRepository.create(m)
        *> ApartmentRepository.getById(m.id, m.modelid, m.company)

  val masterfileAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"get all apartment with modelId ${p._2}   ${p}") *>
        ApartmentRepository.all((p._1, p._2))

  val masterfileByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify apartment  ${p}") *>
        ApartmentRepository.getById(p._1, p._2, p._3)

  val masterfileModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify apartment  ${m}") *>
        ApartmentRepository.modify (m) *>
        ApartmentRepository.getById ((m.id, m.modelid, m.company) )

  val masterfileDeleteRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      ApartmentRepository.delete((id, modelid, company))

  val masterfileRoutes = Routes(masterfileCreateRoute, masterfileAllRoute, masterfileByIdRoute, masterfileModifyRoute
    , masterfileDeleteRoute) @@ Middleware.debug