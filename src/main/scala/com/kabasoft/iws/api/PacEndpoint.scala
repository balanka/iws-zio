package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{pacSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.repository.PacRepository
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.{int, string}
import zio.http.endpoint.{Endpoint, Routes}
import zio.http.Status
import zio.http.endpoint.EndpointMiddleware.None

object PacEndpoint {

  private val allPacAPI       = Endpoint.get("pac"/ string("company")).out[List[PeriodicAccountBalance]]
    .outError[RepositoryError](Status.InternalServerError)
   val pacByAccountPeriodAPI       = Endpoint.get("pac"/string("company")/ string("accId")/int("toPeriod"))
     .out[List[PeriodicAccountBalance]].outError[RepositoryError](Status.InternalServerError)
  val pac4PeriodAPI       = Endpoint.get("pac"/string("company")/int("toPeriod"))
    .out[List[PeriodicAccountBalance]].outError[RepositoryError](Status.InternalServerError)

  private val allPacEndpoint  = allPacAPI.implement(company => PacRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  private val pac4PeriodEndpoint = pac4PeriodAPI.implement(p => PacRepository.getBalances4Period(p._2,  p._1)
    .mapError(e => RepositoryError(e.getMessage)).runCollect.map(_.toList))
 private val pacByAccountPeriodAEndpoint = pacByAccountPeriodAPI.implement{ case (company:String, accId:String, toPeriod:Int) =>
   ZIO.logDebug(s"Get periodic account balance by  accId:  $accId company: ${company}  at: ${toPeriod}") *>{
      PacRepository.find4Period(accId,  toPeriod, company)
       }}

  val appPac: Routes[PacRepository, RepositoryError, None] = allPacEndpoint ++pacByAccountPeriodAEndpoint ++ pac4PeriodEndpoint

}
