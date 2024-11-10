package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Stock}
import com.kabasoft.iws.repository.StockRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, stockSchema, repositoryErrorSchema}
import zio.schema.Schema
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.{path, int, string}
import zio.http.codec.*
import zio.http.endpoint.Endpoint

object StockEndpoint:
  val modelidDoc = "The modelId for identifying the typ of stock "
  val storeIdDoc = "The store id for  selecting the stock"
  val articleIdDoc = "The article id for  selecting the stock"
  val mAllAPIDoc = "Get a stock by company"
  val stockByStoreAndArticleDoc = "Get a stock by store article and company"
  val companyDoc = "The company whom the stock belongs to (i.e. 111111)"
  
  private val mAll = Endpoint(RoutePattern.GET / "stock" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Stock]] ?? Doc.p(mAllAPIDoc)

  private val stockByStoreAndArticle = Endpoint(RoutePattern.GET / "stock" / string("store") ?? Doc.p(storeIdDoc) / string("article") ?? Doc.p(articleIdDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Stock]] ?? Doc.p(stockByStoreAndArticleDoc)


  val stockAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"get all  stock  ${p}") *>
        StockRepository.all((p._1, p._2))

  val stockByStoreAndArticleRoute =
    stockByStoreAndArticle.implement: p =>
      ZIO.logInfo(s"get all  stock for store ${p._1}  and article ${p._2} and company ${p._3}") *>
        StockRepository.getById(p._1, p._2, Stock.MODELID, p._3)
  
  val stockRoutes = Routes(stockByStoreAndArticleRoute, stockAllRoute) @@ Middleware.debug