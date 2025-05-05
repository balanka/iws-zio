package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Masterfile}
import com.kabasoft.iws.repository.MasterfileRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, masterfileSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object MasterfileEndpoint:
  val modelidDoc = "The modelId for identifying the typ of masterfile (i.e. cost center)"
  val idDoc = "The unique Id for identifying the  masterfile"
  val mCreateAPIFoc="Create a new masterfile"
  val mAllAPIDoc = "Get a masterfile by modelId and company"
  val companyDoc = "The company whom the masterfile belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get masterfile by Id and modelId"
  val mModifyAPIDoc = "Modify a masterfile"
  val mDeleteAPIDoc = "Delete a  masterfile"

  private val mCreate = Endpoint(RoutePattern.POST / "mf")
    .in[Masterfile]
    .header(HeaderCodec.authorization)
    .out[Masterfile]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "mf" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  )//.header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Masterfile]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "mf" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Masterfile] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "mf").header(HeaderCodec.authorization)
    .in[Masterfile]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Masterfile] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "mf" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val masterfileCreateRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert masterfile  ${m}") 
        *> MasterfileRepository.create(m)
        *> MasterfileRepository.getById(m.id, m.modelid, m.company)

  val masterfileAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"get all masterfile with modelId ${p._2}   ${p}") *>
        MasterfileRepository.all((p._1, p._2))

  val masterfileByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify masterfile  ${p}") *>
        MasterfileRepository.getById(p._1, p._2, p._3)

  val masterfileModifyRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo (s"Modify masterfile  ${m}") *>
        MasterfileRepository.modify (m) *>
        MasterfileRepository.getById ((m.id, m.modelid, m.company) )

  val masterfileDeleteRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      MasterfileRepository.delete((id, modelid, company))

  val masterfileRoutes = Routes(masterfileCreateRoute, masterfileAllRoute, masterfileByIdRoute, masterfileModifyRoute
    , masterfileDeleteRoute) @@ Middleware.debug