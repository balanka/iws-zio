package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.{AuthenticationError, RepositoryError}
import com.kabasoft.iws.domain.{AppError, Vat}
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, vatSchema}
import com.kabasoft.iws.repository.VatRepository
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object VatEndpoint:
  val modelidDoc = "The modelId for identifying the typ of value added tax"
  val idDoc = "The unique Id for identifying the  value added tax"
  val mCreateAPIFoc="Create a new value added tax"
  val mAllAPIDoc = "Get a value added tax by modelId and company"
  val companyDoc = "The company whom the value added tax belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get value added tax by Id and modelId"
  val mModifyAPIDoc = "Modify a value added tax"
  val mDeleteAPIDoc = "Delete a  value added tax"

  private val vatCreate = Endpoint(RoutePattern.POST / "vat")
    .in[Vat]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val vatAll = Endpoint(RoutePattern.GET / "vat" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Vat]] ?? Doc.p(mAllAPIDoc)

  private val vatById = Endpoint(RoutePattern.GET / "vat" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Vat] ?? Doc.p(mByIdAPIDoc)

  private val vatModify = Endpoint(RoutePattern.PUT / "vat").header(HeaderCodec.authorization)
    .in[Vat]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Vat] ?? Doc.p(mModifyAPIDoc)
  private val vatDelete = Endpoint(RoutePattern.DELETE / "vat" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createRoute =
    vatCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert vat  ${m}") *>
        VatRepository.create(m, true)

  val mAllRoute =
    vatAll.implement : p =>
      ZIO.logInfo(s"Get all vat  ${p}") *>
        VatRepository.all((p._1, p._2))

  val mByIdRoute =
    vatById.implement: p =>
      ZIO.logInfo (s"Modify vat  ${p}") *>
        VatRepository.getById(p._1, p._2, p._3)

  val mModifyRoute =
    vatModify.implement: (h, m) =>
      ZIO.logInfo (s"Modify vat  ${m}") *>
        VatRepository.modify (m) *>
        VatRepository.getById ((m.id, m.modelid, m.company) )

  val mDeleteRoute =
    vatDelete.implement: (id, modelid, company, _)  =>
      VatRepository.delete((id, modelid, company))

  val vatRoutes = Routes(createRoute, mAllRoute, mByIdRoute, mModifyRoute, mDeleteRoute) @@ Middleware.debug
