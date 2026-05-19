package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Transaction}
import com.kabasoft.iws.repository.TransactionRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, transactionSchema}
import com.kabasoft.iws.service.TransactionService

import zio._
import zio.schema.Schema
import zio.http._
import zio.http.codec.PathCodec.{path, int, string, long}
import zio.http.codec._
import zio.http.endpoint.Endpoint

object TransactionEndpoint:
  val modelidDoc = "The modelId for identifying the typ of transaction "
  val idDoc = "The unique Id for identifying the transaction"
  val modelidFromDoc = "The modelId for identifying the type of the transaction (target) which to copy to"
  val modelidToDoc = "The modelId for identifying the type of the transaction (origin) which to copy from "
  val idsDoc = "The list of transaction Id to post"
  val mCreateAPIFoc = "Create a new transaction"
  val mCopyAPIDoc = "Create a new transaction ( i.e. bill  of delivery) from an existing one by copying it (i.e. a customer order)"
  val mAllAPIDoc = "Get a transaction by modelId and company"
  val postAllDoc = "Post all transaction with tge specified ids"
  val companyDoc = "The company whom the transaction belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get transaction by Id and modelId"
  val mModifyAPIDoc = "Modify a transaction"
  val mDeleteAPIDoc = "Delete a transaction"

  private val mCreate = Endpoint(RoutePattern.POST / "ltr")
    .in[Transaction]
    .header(HeaderCodec.authorization)
    .out[Transaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  // copyFromLTr/id/modelidFrom/modelidTo/company
  private val trCopyFrom = Endpoint(RoutePattern.GET / "ltrx" / int("id") ?? Doc.p(idDoc)
    / int("modelidFrom") ?? Doc.p(modelidFromDoc) / int("modelidTo") ?? Doc.p(modelidToDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Transaction] ?? Doc.p(mCopyAPIDoc)

  private val mAll = Endpoint(RoutePattern.GET / "ltr" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[List[Transaction]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "ltr" / long("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Transaction] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "ltr").header(HeaderCodec.authorization)
    .in[Transaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Transaction] ?? Doc.p(mModifyAPIDoc)

  private val trCanceln = Endpoint(RoutePattern.PUT / "cancelnLTr").header(HeaderCodec.authorization)
    .in[Transaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Transaction] ?? Doc.p(mModifyAPIDoc)

  private val trDuplicate = Endpoint(RoutePattern.PUT / "duplicateLTr").header(HeaderCodec.authorization)
    .in[Transaction]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Transaction] ?? Doc.p(mCopyAPIDoc)


  private val mDelete = Endpoint(RoutePattern.DELETE / "ltr" / long("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  private val trPostAll = Endpoint(RoutePattern.GET / "ltr" / "post"/string("transids")?? Doc.p(idsDoc)/ int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Transaction]] ?? Doc.p(postAllDoc)


  val createTransactionRoute =
       mCreate.implement { case (transaction, _) =>
         ZIO.logInfo(s"Insert transaction  ${transaction}") *> TransactionRepository.create(transaction)
       }

  val copyTransactionRoute =
    trCopyFrom.implement: (p) =>
      ZIO.logInfo(s"Copy one transaction from another ${p}") *>
        TransactionService.copyFrom(p._1, p._2, p._3, p._4)


  val trAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Get all transaction  ${p}") *>
        TransactionRepository.all((p._1, p._2))

  val trPostAllRoute =
    trPostAll.implement: p => 
      ZIO.logInfo (s"Post all transaction by id ${p._1.split (',').map (_.toLong).toList}") *>
      TransactionService.postAll (p._1.split (',').map (_.toLong).toList.map (id => (id, p._2) ), p._3) *>
      TransactionRepository.getByIds (p._1.split (',').map (_.toLong).toList, p._2, p._3)
    

  val trByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify transaction  ${p}") *> TransactionRepository.getById(p._1, p._2, p._3)

  val modifyTransactionRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo(s"Modify transaction  ${m}") *> TransactionRepository.modify(m)

  val trCancelnRoute =
    trCanceln.implement: (_, ftr) =>
      ZIO.logInfo(s"Canceln  transaction ${ftr}") *> TransactionRepository.create(ftr.cancel)

  val trDuplicateRoute =
    trDuplicate.implement: (_, ftr) =>
      ZIO.logInfo(s"Duplicate  transaction ${ftr}") *> TransactionRepository.create(ftr.duplicate)

  val deleteTransactionRoute =
     mDelete.implement: (id, modelid, company, _) =>
       TransactionRepository.delete(id, modelid, company)

  val transactionRoutes = Routes(createTransactionRoute, trAllRoute, trPostAllRoute, trByIdRoute, modifyTransactionRoute
    , trDuplicateRoute, trCancelnRoute, deleteTransactionRoute, copyTransactionRoute) @@ Middleware.debug
      