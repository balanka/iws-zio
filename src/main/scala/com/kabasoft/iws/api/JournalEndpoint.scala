package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.{journalSchema, repositoryErrorSchema}
import com.kabasoft.iws.domain.Journal
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.codec.HttpCodec.int
import zio.http.endpoint.Endpoint
import zio.http.Status


object JournalEndpoint {

  val byIdAPI                  = Endpoint.get("journal" / int("id") /string("company")).out[Journal].outError[RepositoryError](Status.InternalServerError)
   val byAccountFromToAPI      = Endpoint.get("journal" / string("company") /string("accId") / int("from") / int("to")).out[List[Journal]].outError[RepositoryError](Status.InternalServerError)
  val journalByIdEndpoint                 = byIdAPI.implement(id => JournalRepository.getBy(id._1.toLong, id._2).mapError(e => RepositoryError(e.getMessage)))
   val journalByAccountFromToEndpoint = byAccountFromToAPI.implement { case (company:String, accId:String, from:Int, to:Int) =>
     ZIO.logInfo(s"Get journal by  accId:  ${accId} company: ${company} from: ${from} to: ${to}") *>
      JournalRepository.find4Period(accId, from, to, company).runCollect.mapBoth(e => RepositoryError(e.getMessage), _.toList)}


   val routesJournal = journalByIdEndpoint ++ journalByAccountFromToEndpoint

  val appJournal = routesJournal//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)
}
