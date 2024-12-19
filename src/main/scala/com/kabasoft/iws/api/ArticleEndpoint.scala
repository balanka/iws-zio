package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.{AuthenticationError, RepositoryError}
import com.kabasoft.iws.domain.{AppError, Article}
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, articleSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository.ArticleRepository
import zio.schema.Schema
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.{path, int, string}
import zio.http.codec.*
import zio.http.endpoint.Endpoint


object ArticleEndpoint:
  val modelidDoc = "The modelId for identifying the typ of article (i.e. cost center)"
  val idDoc = "The unique Id for identifying the  article"
  val mCreateAPIFoc = "Create a new article"
  val mAllAPIDoc = "Get a article by modelId and company"
  val companyDoc = "The company whom the article belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get article by Id and modelId"
  val mModifyAPIDoc = "Modify an article"
  val mDeleteAPIDoc = "Delete an  article"

  private val mCreate = Endpoint(RoutePattern.POST / "art")
    .in[Article]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "art" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[List[Article]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "art" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Article] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "art").header(HeaderCodec.authorization)
    .in[Article]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Article] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "art" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createArtRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert article  ${m}") *>
        ArticleRepository.create(m)

  val allArtRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert article  ${p}") *>
        ArticleRepository.all((p._1, p._2))

  val artByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify article  ${p}") *>
        ArticleRepository.getById(p._1, p._2, p._3)

  val modifyArtRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify article  ${m}") *>
        ArticleRepository.modify(m) *>
        ArticleRepository.getById((m.id, m.modelid, m.company))

  val DeleteArtRoute =
    mDelete.implement: (id, modelid, company, _) =>
      ArticleRepository.delete((id, modelid, company))


  val articleRoutes = Routes(createArtRoute, allArtRoute, artByIdRoute, modifyArtRoute, DeleteArtRoute) @@ Middleware.debug
 
