package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Article
import com.kabasoft.iws.repository.Schema.{articleSchema, repositoryErrorSchema}
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.EndpointMiddleware.None
import zio.http.endpoint.{Endpoint, Routes}

object ArticleEndpoint {

  val articleCreateAPI     = Endpoint.post("art").in[Article].out[Article].outError[RepositoryError](Status.InternalServerError)
  val articleAllAPI        = Endpoint.get("art" / int("modelid")/ string("company")).out[List[Article]].outError[RepositoryError](Status.InternalServerError)
  val articleByIdAPI       = Endpoint.get("art" / string("id")/ string("company")).out[Article].outError[RepositoryError](Status.InternalServerError)
  val articleModifyAPI     = Endpoint.put(literal("art")).in[Article].out[Article].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("art" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val articleAllEndpoint        = articleAllAPI.implement(p =>
    ZIO.logDebug(s"fetch all article  ${p}") *>
     ArticleCache.all(p).mapError(e => RepositoryError(e.getMessage))//.debug("all articles")
    )
  val articleCreateEndpoint = articleCreateAPI.implement(article =>
    ZIO.logDebug(s"Insert article  ${article}") *>
      ArticleRepository.create(article).mapError(e => RepositoryError(e.getMessage)))
  val articleByIdEndpoint = articleByIdAPI.implement( p => ArticleCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val articleModifyEndpoint = articleModifyAPI.implement(p => ZIO.logInfo(s"Modify article  ${p}") *>
    ArticleRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    ArticleRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val articleDeleteEndpoint = deleteAPI.implement(p => ArticleRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

  val appArticle: Routes[ArticleRepository with ArticleCache, RepositoryError, None] =
     articleAllEndpoint ++ articleByIdEndpoint  ++ articleCreateEndpoint ++articleDeleteEndpoint++ articleModifyEndpoint
}
