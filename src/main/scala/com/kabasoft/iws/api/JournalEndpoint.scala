package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Journal, ModelId}
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
  
  // http://127.0.0.1:8091/journal/1000/1810/202300/202312
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
//string("company")?? Doc.p(companyDoc)/string("accountId")?? Doc.p(accountIdDoc) / int("from")?
  val journalByAccountFromToRoute =
    mByAccount4Period.implement { case (company: String, account: String, from: Int, to: Int, _) =>
      for {
        _ <- ZIO.logInfo(s"Get entries for account $account, from $from, to $to, company $company")
        directEntries <- JournalRepository.find4AccountPeriod(account, from, to, company)
        _ <- ZIO.logInfo(s"directEntries $directEntries")
        entries <-
          if (directEntries.nonEmpty) ZIO.succeed(directEntries)
          else
            for {
              subAccounts <- AccountRepository.getByParentId(account, ModelId.ACCOUNT.modelid, company)
              _ <- ZIO.logInfo(s"Sub-accounts: $subAccounts")
              parentEntries <- JournalRepository.find4Period(subAccounts.map(_.id), from, to, company).map(_.toList)
            } yield parentEntries
      } yield entries
    }

  
  val journalRoutes = Routes(journalByPeriodRoute, journalFromPeriod2PerioddRoute, journalByAccountFromToRoute) @@ Middleware.debug


