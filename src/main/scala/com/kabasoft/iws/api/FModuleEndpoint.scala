package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Fmodule}
import com.kabasoft.iws.repository.FModuleRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, fmoduleSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object FModuleEndpoint:
  val modelidDoc = "The modelId for identifying the typ of fmodule "
  val idDoc = "The unique Id for identifying the fmodule"
  val mCreateAPIFoc="Create a new fmodule"
  val mAllAPIDoc = "Get an fmodule by modelId and company"
  val companyDoc = "The company whom the fmodule belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get fmodule by Id and modelId"
  val mModifyAPIDoc = "Modify an fmodule"
  val mDeleteAPIDoc = "Delete an fmodule"

  private val mCreate = Endpoint(RoutePattern.POST / "fmodule")
    .in[Fmodule]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "fmodule" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Fmodule]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "fmodule" / int("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Fmodule] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "fmodule").header(HeaderCodec.authorization)
    .in[Fmodule]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Fmodule] ?? Doc.p(mModifyAPIDoc)
  
  private val mDelete = Endpoint(RoutePattern.DELETE / "fmodule" / int("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createFmoduleRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert fmodule  ${m}") *>
        FModuleRepository.create(m, true)

  val fmoduleAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"Insert fmodule  ${p}") *>
        FModuleRepository.all((p._1, p._2))

  val fmoduleByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify fmodule  ${p}") *>
        FModuleRepository.getById(p._1, p._2, p._3)

  val modifyFmoduleRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo (s"Modify fmodule  ${m}") *>
        FModuleRepository.modify (m) *>
        FModuleRepository.getById ((m.id, m.modelid, m.company) )

  val deleteFmoduleRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      FModuleRepository.delete((id, modelid, company))


  val fmoduleRoutes = Routes(createFmoduleRoute, fmoduleAllRoute, fmoduleByIdRoute, modifyFmoduleRoute, deleteFmoduleRoute) @@ Middleware.debug



