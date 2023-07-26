package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.FinancialsTransaction
import com.kabasoft.iws.repository.Schema._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.FinancialsService
import zio.ZIO
import zio.http.codec.HttpCodec.{int, _}
import zio.http.endpoint.Endpoint
import zio.http.Status

object FinancialsEndpoint {

  val ftrCreateAPI     = Endpoint.post(literal("ftr")).in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
  private val ftrAllAPI        = Endpoint.get("ftr" / string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  private val ftrByModelIdAPI  = Endpoint.get("ftr"/ literal("model")/ string("company")/int("modelid")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  val ftrByTransIdAPI  = Endpoint.get("ftr" / string("company")/ int("transid")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("ftr" / string("company")/ int("transid")).out[Int].outError[RepositoryError](Status.InternalServerError)
  val ftrModifyAPI     = Endpoint.put(literal("ftr")).in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
  private val ftrPostAPI     = Endpoint.get("ftr"/literal("post")/ int("transid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
  private val ftrPost4PeriodAPI     = Endpoint.get("ftr/post"/ string("company")/ int("from") / int("to")).out[Int].outError[RepositoryError](Status.InternalServerError)


  private val ftrAllEndpoint        = ftrAllAPI.implement(company => FinancialsTransactionCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val ftrCreateEndpoint = ftrCreateAPI.implement(ftr =>
    ZIO.logInfo(s"Create Transaction  ${ftr}") *>
      TransactionRepository.create(ftr).mapError(e => RepositoryError(e.getMessage)))
  val ftrByTransIdEndpoint = ftrByTransIdAPI.implement( p =>  ZIO.logInfo(s"Get Transaction by id ${p}") *>
    FinancialsTransactionCache.getByTransId((p._2.toLong, p._1)).mapError(e => RepositoryError(e.getMessage)))
  private val ftrPostEndpoint = ftrPostAPI.implement(p =>  ZIO.logInfo(s"Post Transaction by id ${p}") *>
    FinancialsService.post(p._1.toLong, p._2).mapError(e => RepositoryError(e.getMessage)))
  private val ftrByModelIdEndpoint = ftrByModelIdAPI.implement(p => FinancialsTransactionCache.getByModelId((p._2,p._1)).mapError(e => RepositoryError(e.getMessage)))
  private val ftrPost4PeriodEndpoint = ftrPost4PeriodAPI.implement(p => FinancialsService.postTransaction4Period(p._2, p._3, p._1).mapError(e => RepositoryError(e.getMessage)))
  val ftrModifyEndpoint = ftrModifyAPI.implement(ftr => ZIO.logInfo(s"Modify Transaction  ${ftr}") *>
    TransactionRepository.modify(ftr).mapError(e => RepositoryError(e.getMessage)))
  //PostTransactionRepository.modifyT(ftr).mapError(e => RepositoryError(e.getMessage)))
  private val ftrDeleteEndpoint = deleteAPI.implement(p => TransactionRepository.delete(p._2.toLong, p._1).mapError(e => RepositoryError(e.getMessage)))

  val routes = ftrModifyEndpoint++ftrAllEndpoint  ++ftrByModelIdEndpoint ++ ftrCreateEndpoint ++ftrDeleteEndpoint++
               ftrPostEndpoint++ftrPost4PeriodEndpoint++ ftrByTransIdEndpoint

  val appFtr = routes//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
