package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{Account, AppError, Journal}
import com.kabasoft.iws.repository.{AccountRepository, JournalRepository}
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
  val fromPeriodDoc = "The period to start selecting the journal entries from"
  val tpPeriodDoc = "The period to stop selecting the journal entries to"
  val accountIdDoc = "The account id for which to select the journal entries"
  val fromDoc = "The starting period for selecting the journal entries"
  val toDoc = "The end period for selecting the journal entries"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  val mByPeriodDoc = "Get Journal entries per period and company"
  val mByAccountFromToPeriodDoc = "Get Journal entries for an account and within  period  from/to and company"
  ///journal/5000/202507/202507
  private val mByPeriod = Endpoint(RoutePattern.GET / "journal" / int("period") ?? Doc.p(periodDoc) 
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Journal]] ?? Doc.p(mByPeriodDoc)

  private val mByPeriodFromTo = Endpoint(RoutePattern.GET / "journal" / string("company") ?? Doc.p(companyDoc)
    / int("fromPeriod") ?? Doc.p(fromPeriodDoc) / int("toPeriod") ?? Doc.p(tpPeriodDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Journal]] ?? Doc.p(mByPeriodDoc)
  
// /journal/5500/311000/202500/202507 
  private val mByAccount4Period = Endpoint(RoutePattern.GET / "journal" / string("company")?? Doc.p(companyDoc) 
    /string("accountId")?? Doc.p(accountIdDoc) / int("from")?? Doc.p(fromDoc) / int("to")?? Doc.p(toDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Journal]] ?? Doc.p(mByAccountFromToPeriodDoc)

  val journalByPeriodRoute =
    mByPeriod.implement: (period, company, _) =>
      ZIO.logInfo(s"Get entries 4 period  $period company  $company") *>
        JournalRepository.getByPeriod(period, company)
        
  val journalFromPeriod2PerioddRoute =
    mByPeriodFromTo.implement: (company, fromPeriod, toPeriod, _) =>
      ZIO.logInfo (s"Get entries 4  company  $company from period  $fromPeriod  to   $toPeriod  ") *>
        JournalRepository.getFromPeriod2Period(fromPeriod, toPeriod, company)

  val journalByAccountFromToRoute =
    mByAccount4Period.implement (p =>  for {
    _<- ZIO.logInfo(s"Get entries 4 account  ${p._2}, from ${p._3}, to ${p._4} and  company ${p._1}")
    journalEntries4Account <- JournalRepository.find4Period(p._2, p._3, p._4, p._1)
    accounts <- AccountRepository.getByParentId(p._2, Account.MODELID, p._1)
    journalEntries4Parent <-  if (journalEntries4Account.isEmpty) 
      JournalRepository.find4Period(accounts.map(_.id), p._3, p._4, p._1).map(_.toList)
      else ZIO.succeed(List.empty)
    
  } yield if (journalEntries4Account.nonEmpty) journalEntries4Account else journalEntries4Parent
  )

  
  val journalRoutes = Routes(journalByPeriodRoute, journalFromPeriod2PerioddRoute, journalByAccountFromToRoute) @@ Middleware.debug


