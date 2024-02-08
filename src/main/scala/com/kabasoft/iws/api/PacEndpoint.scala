package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{pacSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.repository.PacRepository
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.{int, string}
import zio.http.endpoint.Endpoint
import zio.http.Status

object PacEndpoint {

  private val allPacAPI       = Endpoint.get("pac"/ string("company")).out[List[PeriodicAccountBalance]]
    .outError[RepositoryError](Status.InternalServerError)
   val pacByAccountPeriodAPI       = Endpoint.get("pac"/string("company")/ string("accId")/int("toPeriod"))
     .out[List[PeriodicAccountBalance]].outError[RepositoryError](Status.InternalServerError)
  val pac4PeriodAPI       = Endpoint.get("pac"/string("company")/int("fromPeriod")/int("toPeriod"))
    .out[List[PeriodicAccountBalance]].outError[RepositoryError](Status.InternalServerError)

  private val allPacEndpoint  = allPacAPI.implement(company => PacRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  private val pac4PeriodEndpoint = pac4PeriodAPI.implement(p => PacRepository.getBalances4Period(p._2, p._3, p._1)
    .mapError(e => RepositoryError(e.getMessage)).runCollect.map(_.toList))
 private val pacByAccountPeriodAEndpoint = pacByAccountPeriodAPI.implement{ case (company:String, accId:String, toPeriod:Int) =>
   ZIO.logInfo(s"Get periodic account balance by  accId:  $accId company: ${company}  at: ${toPeriod}") *>{
      PacRepository.find4Period(accId,  toPeriod, company).runCollect.mapBoth(e => RepositoryError(e.getMessage), _.toList)
       }}

  val routesPac = allPacEndpoint ++pacByAccountPeriodAEndpoint ++ pac4PeriodEndpoint

  val appPac = routesPac//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)


}
