package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Transaction
import com.kabasoft.iws.repository.Schema._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.TransactionService
import zio.ZIO
import zio.http.Status
import zio.http.codec.HttpCodec.{int, _}
import zio.http.endpoint.Endpoint

object TransactionEndpoint {

  val trCreateAPI     = Endpoint.post(literal("ltr")).in[Transaction].out[Transaction].outError[RepositoryError](Status.InternalServerError)
  private val trAllAPI        = Endpoint.get("ltr" / string("company")).out[List[Transaction]].outError[RepositoryError](Status.InternalServerError)
  private val trByModelIdAPI  = Endpoint.get("ltr"/ literal("model")/ string("company")/int("modelid")).out[List[Transaction]].outError[RepositoryError](Status.InternalServerError)
  val trByTransIdAPI  = Endpoint.get("ltr" / string("company")/ int("transid")).out[Transaction].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI = Endpoint.delete("ltr" / string("company")/ int("transid")).out[Int].outError[RepositoryError](Status.InternalServerError)
  val trModifyAPI     = Endpoint.put(literal("ltr")).in[Transaction].out[Transaction].outError[RepositoryError](Status.InternalServerError)
  val trCancelnAPI     = Endpoint.put("cancelnLTr").in[Transaction].out[Transaction].outError[RepositoryError](Status.InternalServerError)
  val trDuplicateAPI     = Endpoint.put("duplicateLTr").in[Transaction].out[Transaction].outError[RepositoryError](Status.InternalServerError)
  //private val ftrPostAPI     = Endpoint.get("ftr"/literal("post")/ int("transid")/string("company")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  private val trPostAllAPI     = Endpoint.get("ltr"/literal("post")/ string("transids")/string("company")).out[List[Transaction]].outError[RepositoryError](Status.InternalServerError)
  //private val trPost4PeriodAPI     = Endpoint.get("ltr/post"/ string("company")/ int("from") / int("to")).out[Int].outError[RepositoryError](Status.InternalServerError)


  private val trAllEndpoint        = trAllAPI.implement(company => TransactionCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val trCreateEndpoint = trCreateAPI.implement(ftr => ZIO.logInfo(s"Create Transaction  ${ftr}") *>
      TransactionRepository.create(ftr).mapError(e => RepositoryError(e.getMessage)))

  val trByTransIdEndpoint = trByTransIdAPI.implement( p =>  ZIO.logDebug(s"Get Transaction by id ${p}") *>
    TransactionRepository.getByTransId((p._2.toLong, p._1)).mapError(e => RepositoryError(e.getMessage)))

  private val trPostAllEndpoint = trPostAllAPI.implement(p => //ZIO.logInfo(s"Post all transaction by id ${p}") *>
    ZIO.logInfo(s"Post all transaction by id ${p._1.split(',').map(_.toLong).toList}") *>
    TransactionService.postAll(p._1.split(',').map(_.toLong).toList, p._2).mapError(e => RepositoryError(e.getMessage)) *>
    TransactionRepository.getByIds(p._1.split(' ').map(_.toLong).toList, p._2).mapError(e => RepositoryError(e.getMessage)))

  private val trCancelnEndpoint = trCancelnAPI.implement(ftr => ZIO.logInfo(s" Canceln  transaction ${ftr}") *>
    TransactionRepository.create(ftr.canceln).mapError(e => RepositoryError(e.getMessage)))
  private val trDuplicateEndpoint = trDuplicateAPI.implement(ftr => ZIO.logInfo(s" Duplicate  transaction ${ftr}") *>
    TransactionRepository.create(ftr.duplicate).mapError(e => RepositoryError(e.getMessage)))

  private val trByModelIdEndpoint = trByModelIdAPI.implement(p =>  ZIO.logDebug(s" get transaction by ModelId ${p}") *>
     TransactionCache.getByModelId((p._2,p._1)).mapError(e => RepositoryError(e.getMessage)))

 // private val trPost4PeriodEndpoint = trPost4PeriodAPI.implement(p => TransactionService.postTransaction4Period(p._2, p._3, p._1).mapError(e => RepositoryError(e.getMessage)))

  val trModifyEndpoint = trModifyAPI.implement(ftr => ZIO.logInfo(s"Modify Transaction  ${ftr}") *>
    TransactionRepository.update(ftr).mapError(e => RepositoryError(e.getMessage)))

  private val trDeleteEndpoint = deleteAPI.implement(p => FinancialsTransactionRepository.delete(p._2.toLong, p._1).mapError(e => RepositoryError(e.getMessage)))

  val appLtr = trModifyEndpoint++trAllEndpoint  ++trByModelIdEndpoint ++ trCreateEndpoint ++trDeleteEndpoint++
               //trPost4PeriodEndpoint++
           trByTransIdEndpoint++trPostAllEndpoint++trCancelnEndpoint++trDuplicateEndpoint

}
