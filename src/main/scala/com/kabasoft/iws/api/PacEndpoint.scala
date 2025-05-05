package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, PeriodicAccountBalance}
import com.kabasoft.iws.repository.PacRepository
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, pacSchema, repositoryErrorSchema}
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema


object PacEndpoint:
  val modelidDoc = "The modelId for identifying the typ of store "
  val accountIdDoc = "The unique Id for identifying the store"
  val periodDoc = "The  period (Format 'YYYYMM') 4 which to select the periodic account balance"
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
  

  val allPacRoute =
    allPac.implement : p =>
      ZIO.logInfo(s"Insert store  ${p}") *>
        PacRepository.all((p._1, p._2))

  val pacByPeriodRoute =
    pacByPeriod.implement: p =>
      ZIO.logInfo(s"Get the PAC entries for period  $p._2 and company $p._1 ") *>
        PacRepository.findBalance4Period(p._2, p._1)
      //pac/106/1000/1810/202101  
      ///pac/1000/1810/202101
      
  val pacByAccountPeriodRoute =
    pacByAccountPeriod.implement: p =>
      ZIO.logInfo (s"Get PAC per  accountId ${p._2}, toPeriod ${p._3} and company ${p._1} ") *>
        PacRepository.find4AccountPeriod(p._2,  p._3, p._1).debug("PAC>>>>>")


  val pacRoutes = Routes(allPacRoute, pacByAccountPeriodRoute, pacByPeriodRoute) @@ Middleware.debug
//  private val allPacAPI       = Endpoint.get("pac"/ int("modelid")/ string("company")).out[List[PeriodicAccountBalance]]
//    .outError[RepositoryError](Status.InternalServerError)
//   val pacByAccountPeriodAPI       = Endpoint.get("pac"/string("company")/ string("accId")/int("toPeriod"))
//     .out[List[PeriodicAccountBalance]].outError[RepositoryError](Status.InternalServerError)
//  val pac4PeriodAPI       = Endpoint.get("pac"/string("company")/int("toPeriod"))
//    .out[List[PeriodicAccountBalance]].outError[RepositoryError](Status.InternalServerError)
//
//  private val allPacEndpoint  = allPacAPI.implement(p => PacRepository.all(p))
//  private val pac4PeriodEndpoint = pac4PeriodAPI.implement(p => PacRepository.findBalance4Period(p._2,  p._1))
// private val pacByAccountPeriodAEndpoint = pacByAccountPeriodAPI.implement{ case (company:String, accId:String, toPeriod:Int) =>
//   ZIO.logDebug(s"Get periodic account balance by  accId:  $accId company: ${company}  at: ${toPeriod}") *>{
//      PacRepository.find4AccountPeriod(accId,  toPeriod, company)
//       }}
//
//  val appPac: Routes[PacRepository, RepositoryError, None] = allPacEndpoint ++pacByAccountPeriodAEndpoint ++ pac4PeriodEndpoint


