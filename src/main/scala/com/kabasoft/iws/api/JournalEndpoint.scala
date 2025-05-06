package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Journal}
import com.kabasoft.iws.repository.JournalRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, journalSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object JournalEndpoint:
  val modelidDoc = "The modelId for identifying the typ of transaction "
  val periodDoc = "The period for selecting the journal entries"
  val accountIdDoc = "The account id for which to select the journal entries"
  val fromDoc = "The starting period for selecting the journal entries"
  val toDoc = "The end period for selecting the journal entries"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  val mByPeriodDoc = "Get Journal entries per period and company"
  val mByAccountFromToPeriodDoc = "Get Journal entries for an account and within  period  from/to and company"
  
  private val mByPeriod = Endpoint(RoutePattern.GET / "journal" / int("period") ?? Doc.p(periodDoc) 
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Journal]] ?? Doc.p(mByPeriodDoc)

  private val mByAccount4Period = Endpoint(RoutePattern.GET / "journal" / string("company")?? Doc.p(companyDoc) 
    /string("accountId")?? Doc.p(accountIdDoc) / int("from")?? Doc.p(fromDoc) / int("to")?? Doc.p(toDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Journal]] ?? Doc.p(mByAccountFromToPeriodDoc)
  
  val journalByPeriodRoute =
    mByPeriod.implement: (period, company, _) =>
      ZIO.logInfo (s"Get entries 4 period  $period company  $company") *>
        JournalRepository.getByPeriod(period, company)

  val journalByAccountFromToRoute =
    mByAccount4Period.implement: (company, accountId, from, to, _) =>
      ZIO.logInfo(s"Get entries 4 account  $accountId, from $from, to $to and  company $company") *>
        JournalRepository.find4Period(accountId, from, to, company)
  
  val journalRoutes = Routes(journalByPeriodRoute, journalByAccountFromToRoute) @@ Middleware.debug


