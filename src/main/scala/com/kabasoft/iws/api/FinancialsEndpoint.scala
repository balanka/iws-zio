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
  
  private val mCreate = Endpoint(RoutePattern.POST / "ftr")
    .in[FinancialsTransaction]
    .header(HeaderCodec.authorization)
    .out[FinancialsTransaction]
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

  private val trCanceln = Endpoint(RoutePattern.PUT / "cancelLTr").header(HeaderCodec.authorization)
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


  val financialsCreateRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert financials transaction  ${m}") 
        *> FinancialsTransactionRepository.create(m)
        *> FinancialsTransactionRepository.getById(m.id, m.modelid, m.company)

  val financialsAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Get financials transaction  ${p}") *>
        FinancialsTransactionRepository.all((p._1, p._2))

  val financialsPostAllRoute =
    trPostAll.implement: p =>
      ZIO.logInfo(s"Post all financials transaction by id ${p._1.split(',').map(_.toLong).toList}") *>
        //TransactionRepository.postAll((p._1, p._2)) *>
        FinancialsTransactionRepository.getBy(p._1.split(',').map(_.toLong).toList, p._2, p._3)

  val financialsByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify financials transaction  ${p}") *>
        FinancialsTransactionRepository.getById(p._1, p._2, p._3)

  val financialsModifyRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify financials transaction  ${m}") *>
        FinancialsTransactionRepository.modify(m) *>
        FinancialsTransactionRepository.getById((m.id, m.modelid, m.company))

  val financialsCancelnRoute =
    trCanceln.implement: (h, ftr) =>
      ZIO.logInfo(s"Canceln  financials transaction ${ftr}") *>
        FinancialsTransactionRepository.create(ftr.cancel) *>
        FinancialsTransactionRepository.getById((ftr.id, ftr.modelid, ftr.company))

  val financialsDuplicateRoute =
    trDuplicate.implement: (h, ftr) =>
      ZIO.logInfo(s"Duplicate  transaction ${ftr}") *>
        FinancialsTransactionRepository.create(ftr.duplicate) *>
        FinancialsTransactionRepository.getById((ftr.id, ftr.modelid, ftr.company))

  val financialsDeleteRoute =
    mDelete.implement: (id, modelid, company, _) =>
      FinancialsTransactionRepository.delete(id, modelid, company)

  val financialsRoutes = Routes(financialsCreateRoute, financialsAllRoute, financialsPostAllRoute, financialsByIdRoute
    , financialsModifyRoute, financialsDuplicateRoute, financialsCancelnRoute, financialsDeleteRoute) @@ Middleware.debug

