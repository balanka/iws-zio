package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.{AuthenticationError, RepositoryError}
import com.kabasoft.iws.domain.{Account, AppError}
import com.kabasoft.iws.repository.AccountRepository
import com.kabasoft.iws.repository.Schema.{accountSchema, authenticationErrorSchema, repositoryErrorSchema}
import com.kabasoft.iws.service.AccountService
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.schema.*
import zio.http.endpoint.Endpoint


object AccountEndpoint:
  val modelidDoc = "The modelId for identifying the typ of account "
  val idDoc = "The unique Id for identifying the account"
  val periodDoc = "The accounting period for which to get the balance"
  val mCreateAPIFoc = "Create a new account"
  val mAllAPIDoc = "Get an account by modelId and company"
  val companyDoc = "The company whom the account belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get an account by Id and modelId"
  val mModifyAPIDoc = "Modify an  account"
  val mcloseAccPeriod = "Close an  accounting period"
  val balance4AccPeriod = "Get balance for accounting period"
  val mDeleteAPIDoc = "Delete an  account"

  private val mCreate = Endpoint(RoutePattern.POST / "acc")
    .in[Account]
    .header(HeaderCodec.authorization)
    .out[Account]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "acc" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Account]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "acc" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Account] ?? Doc.p(mByIdAPIDoc)
  
  val balanceAPI = Endpoint(RoutePattern.GET / "balance" / string("company")?? Doc.p(companyDoc)/string("accId")
    ?? Doc.p(idDoc) / int("to")?? Doc.p(periodDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
                        HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[List[Account]]?? Doc.p(balance4AccPeriod)
  private val mModify = Endpoint(RoutePattern.PUT / "acc").header(HeaderCodec.authorization)
    .in[Account]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Account] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "acc" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val closePeriod = Endpoint(RoutePattern.GET / "close" / string("accId")?? Doc.p(idDoc) / int("to")?? Doc.p(periodDoc)
       / string("company")?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
       .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
       HttpCodec.error[AuthenticationError](Status.Unauthorized)
       ).out[Int]?? Doc.p(mcloseAccPeriod)

  val accountCreateRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert an account  ${m}")
        *>  AccountRepository.create(m)
        *>  AccountRepository.getById(m.id, m.modelid, m.company)

  val accountAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Get all accounts  ${p}") *>
        AccountRepository.all((p._1, p._2))

  val accountByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify an account  ${p}") *>
        AccountRepository.getById(p._1, p._2, p._3)

  val accountBalanceRoute = 
    balanceAPI.implement:  (company:String, accId: String, to: Int, _) =>
      ZIO.logInfo(s"Get the balance  sheet period at ${to} for account: ${accId}") *>
      AccountService.getBalance(accId,  to, company)    

  val accountModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo(s"Modify an account  ${m}") *>
        AccountRepository.modify(m) *>
        AccountRepository.getById((m.id, m.modelid, m.company))
      
  val accountClosePeriodRoute = closePeriod.implement:  (accId: String,  to: Int, company:String, _) =>
        ZIO.logInfo(s"closing period at  ${to}  ${accId}") *>
          AccountService.closePeriod(to, accId, company)
  
  val accountDeleteRoute =
    mDelete.implement: (id, modelid, company, _) =>
      AccountRepository.delete((id, modelid, company))

  val AccountRoutes = Routes(accountCreateRoute, accountAllRoute, accountByIdRoute, accountBalanceRoute
    , accountModifyRoute, accountClosePeriodRoute, accountDeleteRoute) @@ Middleware.debug

