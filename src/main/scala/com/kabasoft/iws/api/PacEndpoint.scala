package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, PeriodicAccountBalance}
import com.kabasoft.iws.repository.PacRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, pacSchema, repositoryErrorSchema}
import com.kabasoft.iws.service.PacService
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema


object PacEndpoint:
  val modelidDoc = "The modelId for identifying the typ of store "
  val accountIdDoc = "The unique Id for identifying the store"
  val periodDoc = "The  period (Format 'YYYYMM') 4 which to select the periodic account balance from "
  val periodToDoc = "The  period (Format 'YYYYMM') 4 which to select the periodic account balance to "
  val periodFromToDoc = "The end period (Format 'YYYYMM') to which to select the periodic account balance  starting  from the period  YYYY01"
  val mAllAPIDoc = "Get periodic account balance by modelId and company"
  val pacByAccountPeriodDoc = "Get periodic account balance for an account and for the period ranging from/to"
  val pacByPeriodDoc = "Get periodic account balance  for the period and company"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"

  
  private val allPac = Endpoint(RoutePattern.GET / "pac" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[PeriodicAccountBalance]] ?? Doc.p(mAllAPIDoc)
// http://localhost:8091/pac/106/1000/1810/202101  
  private val pacByAccountPeriod = Endpoint(RoutePattern.GET / "pac" /string("company") ?? Doc.p(companyDoc) 
    /string("accountId") ?? Doc.p(accountIdDoc)
    / int("fromPeriod") ?? Doc.p(periodFromToDoc)
    / int("toPeriod") ?? Doc.p(periodFromToDoc)
    ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[PeriodicAccountBalance]] ?? Doc.p(pacByAccountPeriodDoc)

  private val pacByPeriod = Endpoint(RoutePattern.GET / "pac" / string("company") ?? Doc.p(companyDoc)
    / int("period") ?? Doc.p(periodDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[PeriodicAccountBalance]] ?? Doc.p(pacByPeriodDoc)

  private val pacByPeriodFromTo = Endpoint(RoutePattern.GET / "pac" / string("company") ?? Doc.p(companyDoc)
    / int("periodFrom") ?? Doc.p(periodDoc)  / int("periodTo") ?? Doc.p(periodToDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[PeriodicAccountBalance]] ?? Doc.p(pacByPeriodDoc)

  val allPacRoute =
    allPac.implement : p =>
      ZIO.logInfo(s"Insert store  ${p}") *>
        PacRepository.all((p._1, p._2))

  val pacByPeriodRoute =
    pacByPeriod.implement: p =>
      ZIO.logInfo(s"Get the PAC entries for period  $p._2 and company $p._1 ") *>
        PacRepository.findBalance4Period(p._2, p._1)

  ///pac/1000/202100/202112
  val pacByPeriodFromToRoute =
    pacByPeriodFromTo.implement: p =>
      ZIO.logInfo(s"Get the PAC entries for period  from $p._2 to $p._3 and company $p._1 ") *>
        PacRepository.findBalance4Period(p._2, p._3, p._1 )
        //PacService.getBalance4Parent(p._2, p._3, p._4, p._1 )


      //pac/106/1000/1810/202101  
      ///pac/1000/1810/202101
  val pacByAccountPeriodRoute =
    pacByAccountPeriod.implement (p =>  for {
      _          <- ZIO.logInfo(s"Get PAC per  accountId ${p._2}, toPeriod ${p._3} and company ${p._1} ")
      pacs       <- PacRepository.find4AccountPeriod(p._2, p._3, p._4, p._1)
      parentPacs <- PacService.getBalance4Parent(p._2, p._3, p._4, p._1)
   } yield  if(pacs.nonEmpty) pacs else parentPacs
  )
  
  val pacRoutes = Routes(allPacRoute, pacByAccountPeriodRoute, pacByPeriodRoute, pacByPeriodFromToRoute) @@ Middleware.debug

