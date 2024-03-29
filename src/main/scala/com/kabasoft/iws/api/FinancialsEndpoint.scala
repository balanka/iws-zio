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

  val ftrCreateAPI     = Endpoint.post(literal("ftr")).in[FinancialsTransaction].out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  private val ftrAllAPI        = Endpoint.get("ftr" / string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  private val ftrByModelIdAPI  = Endpoint.get("ftr"/ literal("model")/ string("company")/int("modelid")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  val ftrByTransIdAPI  = Endpoint.get("ftr" / string("company")/ int("transid")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("ftr" / string("company")/ int("transid")).out[Int].outError[RepositoryError](Status.InternalServerError)
  val ftrModifyAPI     = Endpoint.put(literal("ftr")).in[FinancialsTransaction].out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  val ftrCancelnAPI     = Endpoint.put("cancelnFtr").in[FinancialsTransaction].out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  val ftrDuplicateAPI     = Endpoint.put("duplicateFtr").in[FinancialsTransaction].out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  //private val ftrPostAPI     = Endpoint.get("ftr"/literal("post")/ int("transid")/string("company")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  private val ftrPostAllAPI     = Endpoint.get("ftr"/literal("post")/ string("transids")/string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  private val ftrPost4PeriodAPI     = Endpoint.get("ftr/post"/ string("company")/ int("from") / int("to")).out[Int].outError[RepositoryError](Status.InternalServerError)


  private val ftrAllEndpoint        = ftrAllAPI.implement(company => FinancialsTransactionCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val ftrCreateEndpoint = ftrCreateAPI.implement(ftr => ZIO.logInfo(s"Create Transaction  ${ftr}") *>
      FinancialsTransactionRepository.create(ftr).mapError(e => RepositoryError(e.getMessage)))

  val ftrByTransIdEndpoint = ftrByTransIdAPI.implement( p =>  ZIO.logInfo(s"Get Transaction by id ${p}") *>
    FinancialsTransactionRepository.getByTransId((p._2.toLong, p._1)).mapError(e => RepositoryError(e.getMessage)))

//  private val ftrPostEndpoint = ftrPostAPI.implement(p =>  ZIO.logInfo(s"Post Transaction by id ${p}") *>
//    FinancialsService.post(p._1.toLong, p._2).mapError(e => RepositoryError(e.getMessage))*>
//    TransactionRepository.getByTransId((p._1.toLong, p._2)).mapError(e => RepositoryError(e.getMessage)))

  private val ftrPostAllEndpoint = ftrPostAllAPI.implement(p => //ZIO.logInfo(s"Post all transaction by id ${p}") *>
    ZIO.logInfo(s"Post all transaction by id ${p._1.split(',').map(_.toLong).toList}") *>
    FinancialsService.postAll(p._1.split(',').map(_.toLong).toList, p._2).mapError(e => RepositoryError(e.getMessage)) *>
    FinancialsTransactionRepository.getByIds(p._1.split(' ').map(_.toLong).toList, p._2).mapError(e => RepositoryError(e.getMessage)))

  private val ftrCancelnEndpoint = ftrCancelnAPI.implement(ftr => ZIO.logInfo(s" Canceln  transaction ${ftr}") *>
    FinancialsTransactionRepository.create(ftr.canceln).mapError(e => RepositoryError(e.getMessage)))
  private val ftrDuplicateEndpoint = ftrDuplicateAPI.implement(ftr => ZIO.logInfo(s" Duplicate  transaction ${ftr}") *>
    FinancialsTransactionRepository.create(ftr.duplicate).mapError(e => RepositoryError(e.getMessage)))

  private val ftrByModelIdEndpoint = ftrByModelIdAPI.implement(p => FinancialsTransactionCache.getByModelId((p._2,p._1)).mapError(e => RepositoryError(e.getMessage)))

  private val ftrPost4PeriodEndpoint = ftrPost4PeriodAPI.implement(p => FinancialsService.postTransaction4Period(p._2, p._3, p._1).mapError(e => RepositoryError(e.getMessage)))

  val ftrModifyEndpoint = ftrModifyAPI.implement(ftr => ZIO.logInfo(s"Modify Transaction  ${ftr}") *>
    FinancialsTransactionRepository.update(ftr).mapError(e => RepositoryError(e.getMessage)))

  private val ftrDeleteEndpoint = deleteAPI.implement(p => FinancialsTransactionRepository.delete(p._2.toLong, p._1).mapError(e => RepositoryError(e.getMessage)))

  val appFtr = ftrModifyEndpoint++ftrAllEndpoint  ++ftrByModelIdEndpoint ++ ftrCreateEndpoint ++ftrDeleteEndpoint++
               ftrPost4PeriodEndpoint++ ftrByTransIdEndpoint++ftrPostAllEndpoint++ftrCancelnEndpoint++ftrDuplicateEndpoint

}
