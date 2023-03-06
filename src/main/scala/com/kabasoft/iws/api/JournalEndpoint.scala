package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.journalSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Journal
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.codec.HttpCodec.int
import zio.http.endpoint.Endpoint
import zio.http.model.Status


object JournalEndpoint {

  val byIdAPI                  = Endpoint.get("jou" / int("id")).out[Journal].outError[RepositoryError](Status.InternalServerError)
   val byAccountFromToAPI      = Endpoint.get("jou" / string("accId") / int("from") / int("to")).out[List[Journal]].outError[RepositoryError](Status.InternalServerError)
  val journalByIdEndpoint                 = byIdAPI.implement(id => JournalRepository.getBy(id.toLong, "1000").mapError(e => RepositoryError(e.getMessage)))
   val journalByAccountFromToEndpoint = byAccountFromToAPI.implement { case (accId:String, from:Int,to:Int) =>
      JournalRepository.find4Period(accId, from, to, "1000").runCollect.mapBoth(e => RepositoryError(e.getMessage), _.toList)}


   val routesJournal = journalByIdEndpoint ++ journalByAccountFromToEndpoint

  val appJournal = routesJournal//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)
}
