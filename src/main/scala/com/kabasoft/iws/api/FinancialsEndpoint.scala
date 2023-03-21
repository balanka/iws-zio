package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import com.kabasoft.iws.repository.Schema._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.FinancialsService
import zio.ZIO
import zio.http.codec.HttpCodec.{int, _}
import zio.http.endpoint.Endpoint
import zio.http.model.Status

object FinancialsEndpoint {

  private val ftrCreateAPI     = Endpoint.post("ftr").in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
  private val ftrAllAPI        = Endpoint.get("ftr" / string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  private val ftrByModelidAPI  = Endpoint.get("ftr/model" /int("modelid") /string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  private val ftrByTransIdAPI  = Endpoint.get("ftr" / int("transid")/ string("company")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("ftr" / int("transid")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
  private val ftrPostAPI     = Endpoint.get("ftr/post"/int("transid")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
  private val ftrPost4PeriodAPI     = Endpoint.get("ftr/post"/ int("from") / int("to")/ string("company")).out[List[Int]].outError[RepositoryError](Status.InternalServerError)


  private val ftrAllEndpoint        = ftrAllAPI.implement(company => FinancialsTransactionCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  private val ftrCreateEndpoint = ftrCreateAPI.implement(ftr =>
    ZIO.logDebug(s"Insert Transaction  ${ftr}") *>
      TransactionRepository.create(ftr).mapError(e => RepositoryError(e.getMessage)))
  private val ftrByTransIdEndpoint = ftrByTransIdAPI.implement( p => FinancialsTransactionCache.getByTransId((p._1.toLong, p._2)).mapError(e => RepositoryError(e.getMessage)))
  private val ftrPostEndpoint = ftrPostAPI.implement(p => FinancialsService.post(p._1.toLong, p._2).mapError(e => RepositoryError(e.getMessage)))
  private val ftrByModelidEndpoint = ftrByModelidAPI.implement(p => FinancialsTransactionCache.getByModelId(p).mapError(e => RepositoryError(e.getMessage)))
  private val ftrPost4PeriodEndpoint = ftrPost4PeriodAPI.implement(p => FinancialsService.postTransaction4Period(p._1, p._2, p._3).mapError(e => RepositoryError(e.getMessage)))
  private val ftrDeleteEndpoint = deleteAPI.implement(p => TransactionRepository.delete(p._1.toLong, p._2).mapError(e => RepositoryError(e.getMessage)))

  val routes = ftrAllEndpoint ++ ftrByTransIdEndpoint ++ftrByModelidEndpoint ++ ftrCreateEndpoint ++ftrDeleteEndpoint++ ftrPostEndpoint++ftrPost4PeriodEndpoint

  val appBank = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
