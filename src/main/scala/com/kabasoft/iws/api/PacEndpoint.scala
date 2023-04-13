package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.pacSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.repository.PacRepository
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.{int, string}
import zio.http.endpoint.Endpoint
import zio.http.Status

object PacEndpoint {

  private val allPacAPI       = Endpoint.get("pac"/ string("company")).out[List[PeriodicAccountBalance]]
    .outError[RepositoryError](Status.InternalServerError)
   val pacByAccountPeriodAPI       = Endpoint.get("pac"/string("company")/ string("accId")/int("fromPeriod")/int("toPeriod"))
     .out[List[PeriodicAccountBalance]].outError[RepositoryError](Status.InternalServerError)

  //private val pacByIdAPI      = Endpoint.get("pac" / string("id")).out[PeriodicAccountBalance].outError[RepositoryError](Status.InternalServerError)
  private val allPacEndpoint  = allPacAPI.implement(company => PacRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
 // private val pacByIdEndpoint = pacByIdAPI.implement(id => PacRepository.getBy(id, "1000").mapError(e => RepositoryError(e.getMessage)))
   private val pacByAccountPeriodAEndpoint = pacByAccountPeriodAPI.implement{ case (company:String, accId:String, fromPeriod:Int,toPeriod:Int) =>
     PacRepository.find4Period(accId, fromPeriod, toPeriod, company).runCollect.mapBoth(e => RepositoryError(e.getMessage), _.toList)}

  val routesPac = allPacEndpoint ++pacByAccountPeriodAEndpoint

  val appPac = routesPac//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)


}
