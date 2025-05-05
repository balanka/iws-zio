package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Transaction}
import com.kabasoft.iws.repository.TransactionRepository
import com.kabasoft.iws.service.TransactionService
import com.kabasoft.iws.api.Protocol.*
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, transactionSchema,
          transactionDetailsSchema,repositoryErrorSchema}
import zio.*
import zio.http.RoutePattern
import zio.schema.annotation.description
import zio.schema.{DeriveSchema, Schema}
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.{path, int, string, long}
import zio.http.codec.*
import zio.http.endpoint.AuthType.None
import zio.http.endpoint.{AuthType, Endpoint}
import zio.http.endpoint.openapi.{OpenAPIGen, SwaggerUI}

object TransactionEndpoint:
  val modelidDoc = "The modelId for identifying the typ of transaction "
  val idDoc = "The unique Id for identifying the transaction"
  val idsDoc = "The list of transaction Id to post"
  val mCreateAPIFoc = "Create a new store"
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
    ).out[Transaction] ?? Doc.p(mModifyAPIDoc)


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
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert transaction  ${m}")
      *> TransactionRepository.create(m)
      *> TransactionRepository.getById(m.id, m.modelid, m.company)

  val trAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert transaction  ${p}") *>
        TransactionRepository.all((p._1, p._2))

  val trPostAllRoute =
    trPostAll.implement: p =>
      ZIO.logInfo(s"Post all transaction by id ${p._1.split(',').map(_.toLong).toList}") *>
        //TransactionRepository.postAll((p._1, p._2)) *>
        TransactionRepository.getByIds(p._1.split(',').map(_.toLong).toList, p._2, p._3)

  val trByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify transaction  ${p}") *>
        TransactionRepository.getById(p._1, p._2, p._3)

  val modifyTransactionRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify transaction  ${m}") *>
        TransactionRepository.modify(m) *>
        TransactionRepository.getById((m.id, m.modelid, m.company))

  val trCancelnRoute =
    trCanceln.implement: (h, ftr) =>
      ZIO.logInfo(s"Canceln  transaction ${ftr}") *>
        TransactionRepository.create(ftr.cancel)  *>
        TransactionRepository.getById((ftr.id, ftr.modelid, ftr.company))

  val trDuplicateRoute =
    trDuplicate.implement: (h, ftr) =>
      ZIO.logInfo(s"Duplicate  transaction ${ftr}") *>
        TransactionRepository.create(ftr.duplicate) *>
        TransactionRepository.getById((ftr.id, ftr.modelid, ftr.company))

  val deleteTransactionRoute =
     mDelete.implement: (id, modelid, company, _) =>
       TransactionRepository.delete(id, modelid, company)

  val transactionRoutes = Routes(createTransactionRoute, trAllRoute, trPostAllRoute, trByIdRoute, modifyTransactionRoute
    , trDuplicateRoute, trCancelnRoute, deleteTransactionRoute) @@ Middleware.debug

