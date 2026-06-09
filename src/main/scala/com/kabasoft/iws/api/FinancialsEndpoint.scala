package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, FinancialsTransaction, ReminderBalance}
import com.kabasoft.iws.repository.FinancialsTransactionRepository
import com.kabasoft.iws.service.FinancialsService
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, ftransactionSchema, reminderBalance, repositoryErrorSchema}
import zio.*
import zio.http.RoutePattern
import zio.schema.Schema
import zio.http.*
import zio.http.codec.PathCodec.{int, long, path, string}
import zio.http.codec.*
import zio.http.endpoint.Endpoint

object FinancialsEndpoint:
  val modelidDoc = "The modelId for identifying the typ of financials transaction (Customer / Vendor invoice, setllement, payment, etc... )"
  val modelidFromDoc = "The modelId for identifying the typ of financials transaction (target) which to copy to"
  val modelidToDoc = "The modelId for identifying the typ of financials transaction (origin) which to copy from "
  val idDoc = "The unique Id for identifying the financials transaction"
  val idsDoc = "The list of financials transaction's Id to post or to fetch from DB"
  val accountIdDoc = "Receivable account id for finding for reminding balance due"
  val mCreateAPIFoc = "Create a new financials transaction (Customer / Ventor invoice, setllement, payment, etc... )"
  val mAllAPIDoc = "Get a financials transaction by modelId and company"
  val postAllDoc = "Post all financials transaction with the specified ids and type"
  val companyDoc = "The company whom the financials transaction belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get financials transaction by its Id and modelId"
  val mModifyAPIDoc = "Modify a financials transaction"
  val mDuplicateAPIDoc = "Copy/duplicate a financials transaction"
  val mCopyAPIDoc = "Copy a financials transaction from another one"
  val mDeleteAPIDoc = "Delete a financials transaction"
  val postAPIDoc = "Post a financials transaction"
  val postAllAPIDoc = "Post a set of financials transaction with specified ids"
  val mByIdsAPIDoc = "Fetch a set of financials transaction with specified ids"
  
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

  private val mAlln = Endpoint(RoutePattern.GET / "ftr" /"n"/ string("company") ?? Doc.p(companyDoc)
    / string("modelid") ?? Doc.p(modelidDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[List[FinancialsTransaction]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "ftr" / long("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mByIdAPIDoc)

  private val balanceByPaiementRemeinder = Endpoint(RoutePattern.GET / "ftr" /"balance"/ string("accountId") ?? Doc.p(accountIdDoc) /
     string("companyId") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[List[ReminderBalance]] ?? Doc.p(mByIdAPIDoc)
  

  private val mModify = Endpoint(RoutePattern.PUT / "ftr").header(HeaderCodec.authorization)
    .in[FinancialsTransaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mModifyAPIDoc)

  private val trCanceln = Endpoint(RoutePattern.PUT / "cancelFTr").header(HeaderCodec.authorization)
    .in[FinancialsTransaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mModifyAPIDoc)

  private val trDuplicate = Endpoint(RoutePattern.PUT / "duplicateFTr").header(HeaderCodec.authorization)
    .in[FinancialsTransaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mModifyAPIDoc)
  // ftrx/id/modelidFrom/modelidTo/company
  private val trCopyFrom = Endpoint(RoutePattern.GET / "ftrx"/int("id") ?? Doc.p(idDoc)
    /int("modelidFrom") ?? Doc.p(modelidFromDoc) / int("modelidTo")?? Doc.p(modelidToDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[FinancialsTransaction] ?? Doc.p(mCopyAPIDoc)
  
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
  
  val financialsCreateRoute = mCreate.implement { p =>
      val transaction = p._1
      ZIO.logInfo(s"Insert financials transaction  ${transaction}") *>
      FinancialsTransactionRepository.create(transaction)
  }
  val financialsAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Get all financials transaction  for modelid: ${p._1}") *>
        FinancialsTransactionRepository.all((p._1, p._2))

  val financialsAllNRoute =
    mAlln.implement: p =>
      ZIO.logInfo(s"Get all financials transaction  for modelid: ${p._2.split(',').map(_.trim.toInt).toList}") *>
        FinancialsTransactionRepository.alln((p._2.split(',').map(_.trim.toInt).toList, p._1))

  val financialsPostAllRoute =
    trPostAll.implement: p =>
      ZIO.logInfo(s"Post all financials transaction by id ${p._1.split('*').map(_.toLong).toList}") *>
        FinancialsService.postAll({p._1.split('*').map(_.toLong).toList}, p._2, p._3) *>
        FinancialsTransactionRepository.getBy(p._1.split('*').map(_.toLong).toList, p._2, p._3)

  val financialsByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Get financials transaction by id  ${p._1} modelid ${p._2} company ${p._3} ") *>
        FinancialsTransactionRepository.getById(p._1, p._2, p._3)
  
  val financialsModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo(s"Modify financials transaction  ${m}") *>
        FinancialsTransactionRepository.modify(m)
  
  val financialsCancelnRoute =
    trCanceln.implement: (_, ftr) =>
      ZIO.logInfo(s"Canceln  financials transaction ${ftr}") *>
        FinancialsTransactionRepository.create(ftr.cancel)
//// copyFromFTr/id/modelidFrom/modelidTo/company
  val financialsCopyFromRoute =
    trCopyFrom.implement: (p) =>
      ZIO.logInfo(s"Copy one transaction from another ${p}") *>
        FinancialsService.copyFrom(p._1, p._2, p._3, p._4)
        
  val financialsDuplicateRoute =
    trDuplicate.implement: (_, ftr) =>
      ZIO.logInfo(s"Duplicate  transaction ${ftr}") *>
        FinancialsTransactionRepository.create(ftr.duplicate) 
  
  val financialsDeleteRoute =
    mDelete.implement: (id, modelid, company, _) =>
      FinancialsTransactionRepository.delete(id, modelid, company)

  val balanceByPaiementRemeinderRoute =
    balanceByPaiementRemeinder.implement: (accountId, companyId, _) =>
      FinancialsService.findBalance4paymentReminder(accountId, companyId)


  val financialsRoutes = Routes(financialsCreateRoute, financialsAllRoute, financialsPostAllRoute, financialsByIdRoute
    , financialsAllNRoute, financialsModifyRoute, financialsDuplicateRoute, financialsCancelnRoute, financialsDeleteRoute
    , financialsCopyFromRoute, balanceByPaiementRemeinderRoute)
    @@ Middleware.debug

