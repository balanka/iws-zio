package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, FinancialsTransaction}
import com.kabasoft.iws.repository.FinancialsTransactionRepository
import com.kabasoft.iws.service.TransactionService
import com.kabasoft.iws.api.Protocol.*
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, ftransactionDetailsSchema, ftransactionSchema, repositoryErrorSchema, transactionDetailsSchema, transactionSchema}
import zio.*
import zio.http.RoutePattern
import zio.schema.annotation.description
import zio.schema.{DeriveSchema, Schema}
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.{int, long, path, string}
import zio.http.codec.*
import zio.http.endpoint.AuthType.None
import zio.http.endpoint.{AuthType, Endpoint}
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}


object FinancialsEndpoint:
  val modelidDoc = "The modelId for identifying the typ of financials transaction (Customer / Ventor invoice, setllement, payment, etc... )"
  val idDoc = "The unique Id for identifying the financials transaction"
  val idsDoc = "The list of financials transaction's Id to post"
  val mCreateAPIFoc = "Create a new financials transaction (Customer / Ventor invoice, setllement, payment, etc... )"
  val mAllAPIDoc = "Get a financials transaction by modelId and company"
  val postAllDoc = "Post all financials transaction with the specified ids and type"
  val companyDoc = "The company whom the financials transaction belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get financials transaction by its Id and modelId"
  val mModifyAPIDoc = "Modify a financials transaction"
  val mDeleteAPIDoc = "Delete a financials transaction"
  val postAPIDoc = "Post a financials transaction"
  val postAllAPIDoc = "Post a set of financials transaction with specified ids"

  // financialsRoutes
  //  val ftrCreateAPI     = Endpoint.post(literal("ftr")).in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
  //  private val ftrAllAPI        = Endpoint.get("ftr" / int("modelid")/string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  //  private val ftrByModelIdAPI  = Endpoint.get("ftr"/ literal("model")/ string("company")/int("modelid")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  //  val ftrByTransIdAPI  = Endpoint.get("ftr" / string("company")/ int("transid")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  //  private val deleteAPI = Endpoint.delete("ftr" / string("company")/ int("transid")/int("modelid")).out[(Long, Int, String)].outError[RepositoryError](Status.InternalServerError)
  //  val ftrModifyAPI     = Endpoint.put(literal("ftr")).in[FinancialsTransaction].out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  //  val ftrCancelnAPI     = Endpoint.put("cancelnFtr").in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
  //  val ftrDuplicateAPI     = Endpoint.put("duplicateFtr").in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
  //  //private val ftrPostAPI     = Endpoint.get("ftr"/literal("post")/ int("transid")/string("company")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
  //  private val ftrPostAllAPI     = Endpoint.get("ftr"/literal("post")/ string("transids")/string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
  //  private val ftrPost4PeriodAPI     = Endpoint.get("ftr/post"/ string("company")/ int("from") / int("to")/int("modelid")).out[Int].outError[RepositoryError](Status.InternalServerError)
  //

  private val mCreate = Endpoint(RoutePattern.POST / "ftr")
    .in[FinancialsTransaction]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "ftr" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[List[FinancialsTransaction]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "ftr" / long("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "ftr").header(HeaderCodec.authorization)
    .in[FinancialsTransaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mModifyAPIDoc)

  private val trCanceln = Endpoint(RoutePattern.PUT / "cancelnLTr").header(HeaderCodec.authorization)
    .in[FinancialsTransaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mModifyAPIDoc)

  private val trDuplicate = Endpoint(RoutePattern.PUT / "duplicateLTr").header(HeaderCodec.authorization)
    .in[FinancialsTransaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mModifyAPIDoc)


  private val mDelete = Endpoint(RoutePattern.DELETE / "ftr" / long("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  private val trPostAll = Endpoint(RoutePattern.GET / "ftr" / "post" / string("transids") ?? Doc.p(idsDoc) / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[FinancialsTransaction]] ?? Doc.p(postAllDoc)


  val createTransactionRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert financials transaction  ${m}") *>
        FinancialsTransactionRepository.create(m)

  val trAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert financials transaction  ${p}") *>
        FinancialsTransactionRepository.all((p._1, p._2))

  val trPostAllRoute =
    trPostAll.implement: p =>
      ZIO.logInfo(s"Post all financials transaction by id ${p._1.split(',').map(_.toLong).toList}") *>
        //TransactionRepository.postAll((p._1, p._2)) *>
        FinancialsTransactionRepository.getByIds(p._1.split(',').map(_.toLong).toList, p._2, p._3)

  val trByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify financials transaction  ${p}") *>
        FinancialsTransactionRepository.getById(p._1, p._2, p._3)

  val modifyTransactionRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify financials transaction  ${m}") *>
        FinancialsTransactionRepository.modify(m) *>
        FinancialsTransactionRepository.getById((m.id, m.modelid, m.company))

  val trCancelnRoute =
    trCanceln.implement: (h, ftr) =>
      ZIO.logInfo(s"Canceln  financials transaction ${ftr}") *>
        FinancialsTransactionRepository.create(ftr.cancel) *>
        FinancialsTransactionRepository.getById((ftr.id, ftr.modelid, ftr.company))

  val trDuplicateRoute =
    trDuplicate.implement: (h, ftr) =>
      ZIO.logInfo(s"Duplicate  transaction ${ftr}") *>
        FinancialsTransactionRepository.create(ftr.duplicate) *>
        FinancialsTransactionRepository.getById((ftr.id, ftr.modelid, ftr.company))

  val deleteTransactionRoute =
    mDelete.implement: (id, modelid, company, _) =>
      FinancialsTransactionRepository.delete(id, modelid, company)

  val financialsRoutes = Routes(createTransactionRoute, trAllRoute, trPostAllRoute, trByIdRoute, modifyTransactionRoute
    , trDuplicateRoute, trCancelnRoute, deleteTransactionRoute) @@ Middleware.debug


// financialsRoutes
//  val ftrCreateAPI     = Endpoint.post(literal("ftr")).in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
//  private val ftrAllAPI        = Endpoint.get("ftr" / int("modelid")/string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
//  private val ftrByModelIdAPI  = Endpoint.get("ftr"/ literal("model")/ string("company")/int("modelid")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
//  val ftrByTransIdAPI  = Endpoint.get("ftr" / string("company")/ int("transid")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
//  private val deleteAPI = Endpoint.delete("ftr" / string("company")/ int("transid")/int("modelid")).out[(Long, Int, String)].outError[RepositoryError](Status.InternalServerError)
//  val ftrModifyAPI     = Endpoint.put(literal("ftr")).in[FinancialsTransaction].out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
//  val ftrCancelnAPI     = Endpoint.put("cancelnFtr").in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
//  val ftrDuplicateAPI     = Endpoint.put("duplicateFtr").in[FinancialsTransaction].out[Int].outError[RepositoryError](Status.InternalServerError)
//  //private val ftrPostAPI     = Endpoint.get("ftr"/literal("post")/ int("transid")/string("company")).out[FinancialsTransaction].outError[RepositoryError](Status.InternalServerError)
//  private val ftrPostAllAPI     = Endpoint.get("ftr"/literal("post")/ string("transids")/string("company")).out[List[FinancialsTransaction]].outError[RepositoryError](Status.InternalServerError)
//  private val ftrPost4PeriodAPI     = Endpoint.get("ftr/post"/ string("company")/ int("from") / int("to")/int("modelid")).out[Int].outError[RepositoryError](Status.InternalServerError)
//
//
//  private val ftrAllEndpoint        = ftrAllAPI.implement(p => FinancialsTransactionRepository.all(p).mapError(e => RepositoryError(e.getMessage)))
//  val ftrCreateEndpoint = ftrCreateAPI.implement(ftr => ZIO.logInfo(s"Create financials Transaction  ${ftr}") *>
//      FinancialsTransactionRepository.create(ftr).mapError(e => RepositoryError(e.getMessage)))
//
//  val ftrByTransIdEndpoint = ftrByTransIdAPI.implement( p =>  ZIO.logInfo(s"Get financials Transaction by id ${p}") *>
//    FinancialsTransactionRepository.getByTransId((p._2.toLong, p._1)).mapError(e => RepositoryError(e.getMessage)))
//
///*  private val ftrPostEndpoint = ftrPostAPI.implement(p =>  ZIO.logInfo(s"Post Transaction by id ${p}") *>
//    FinancialsService.post(p._1.toLong, p._2).mapError(e => RepositoryError(e.getMessage))*>
//    TransactionRepository.getByTransId((p._1.toLong, p._2)).mapError(e => RepositoryError(e.getMessage)))
// */
//
//  private val ftrPostAllEndpoint = ftrPostAllAPI.implement(p => //ZIO.logInfo(s"Post all transaction by id ${p}") *>
//    ZIO.logInfo(s"Post all financials transaction by id ${p._1.split(',').map(_.toLong).toList}") *>
//    FinancialsService.postAll(p._1.split(',').map(_.toLong).toList, p._2) *>
//    FinancialsTransactionRepository.getByIds(p._1.split(' ').map(_.toLong).toList, p._2))
//
//  private val ftrCancelnEndpoint = ftrCancelnAPI.implement(ftr => ZIO.logInfo(s" Canceln financials transaction ${ftr}") *>
//    FinancialsTransactionRepository.create(ftr.canceln))
//  private val ftrDuplicateEndpoint = ftrDuplicateAPI.implement(ftr => ZIO.logInfo(s" Duplicate financials transaction ${ftr}") *>
//    FinancialsTransactionRepository.create(ftr.duplicate))
//
//  private val ftrByModelIdEndpoint = ftrByModelIdAPI.implement(p => FinancialsTransactionRepository.getByModelId((p._2,p._1)))
//
//  private val ftrPost4PeriodEndpoint = ftrPost4PeriodAPI.implement(p => FinancialsService.postTransaction4Period(p._2, p._3, p._4, p._1)
//    .mapError(e => RepositoryError(e.getMessage)))
//
//  val ftrModifyEndpoint = ftrModifyAPI.implement(ftr => ZIO.logInfo(s"Modify financials Transaction  ${ftr}") *>
//    FinancialsTransactionRepository.modify(ftr) *> FinancialsTransactionRepository.getByTransId((ftr.id, ftr.company)))
//
//  private val ftrDeleteEndpoint = deleteAPI.implement(p => FinancialsTransactionRepository.delete(p._2.toLong, p._3, p._1))
//
//  val appFtr = ftrModifyEndpoint++ftrAllEndpoint  ++ftrByModelIdEndpoint ++ ftrCreateEndpoint ++ftrDeleteEndpoint++
//               ftrPost4PeriodEndpoint++ ftrByTransIdEndpoint++ftrPostAllEndpoint++ftrCancelnEndpoint++ftrDuplicateEndpoint

