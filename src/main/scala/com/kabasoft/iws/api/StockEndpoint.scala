package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Stock
import com.kabasoft.iws.repository.StockRepository
import com.kabasoft.iws.repository.Schema.{stockSchema, repositoryErrorSchema}
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec._
import zio.http.endpoint.EndpointMiddleware.None
import zio.http.endpoint.{Endpoint, Routes}

object StockEndpoint {

  private val allStockAPI       = Endpoint.get("stock"/ int("modelid")/string("company")).out[List[Stock]]
    .outError[RepositoryError](Status.InternalServerError)
   //val stockByStoreOrArticleAPI       = Endpoint.get("stock"/string("company")/ string("id"))
   //  .out[List[Stock]].outError[RepositoryError](Status.InternalServerError)
  val stockByStoreAndArticleAPI       = Endpoint.get("stock"/string("store")/ string("article")/ string("company"))
    .out[Stock].outError[RepositoryError](Status.InternalServerError)


  private val allStockEndpoint  = allStockAPI.implement(company => StockRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
//  val stockByStoreAndArticleEndpoint = stockByStoreOrArticleAPI.implement(p=>
//    ZIO.logDebug(s"Get stock by  store or article :  ${p._1} company: ${p._2}") *> StockRepository.getBy(p).mapError(e => RepositoryError(e.getMessage)))

  val stockByStoreOrArticleEndpoint = stockByStoreAndArticleAPI.implement(p=>
    ZIO.logDebug(s"Get stock by  store or article :  ${p._1} company: ${p._2}") *> StockRepository.getBy((p._1, p._1), p._3).mapBoth(e => RepositoryError(e.getMessage), a=>a.getOrElse(Stock.dummy)))
  val appStock: Routes[StockRepository, RepositoryError, None] = allStockEndpoint ++ stockByStoreOrArticleEndpoint //++stockByStoreAndArticleEndpoint

}
