package com.kabasoft.iws.api

import com.kabasoft.iws.api.Protocol._
import com.kabasoft.iws.domain.{AppError, Journal}
import com.kabasoft.iws.repository._
import zio.http._
import zio.http.api.HttpCodec.literal
import zio.http.api.{EndpointSpec, RouteCodec}
import zio.schema.DeriveSchema.gen

object JournalEndpoint {

   val byIdAPI = EndpointSpec.get[Int](literal("jou")/ RouteCodec.int("id") ).out[Journal]
  private val byAccountFromToAPI = EndpointSpec.get[(String, Int, Int)](literal("jou")
    /RouteCodec.string("accId") /RouteCodec.int("from")/ RouteCodec.int("to")  ).out[List[Journal]]
  //private val deleteAPI = EndpointSpec.get[String](literal("jou")/ RouteCodec.string("id") ).out[Int]
   val jouByIdEndpoint = byIdAPI.implement(id =>JournalRepository.getBy(id.toLong,"1000"))
  private val byAccountFromToEndpoint = byAccountFromToAPI.implement((accId) =>
    JournalRepository.find4Period(accId._1, accId._2, accId._3, "1000").runCollect.map(_.toList))
  //private val deleteEndpoint = deleteAPI.implement (id =>JournalRepository.delete(id.toLong,"1000"))

  private val serviceSpec = ( byIdAPI.toServiceSpec++byAccountFromToAPI.toServiceSpec)

  val appJournal: HttpApp[JournalRepository, AppError.RepositoryError] =
    serviceSpec.toHttpApp( jouByIdEndpoint++byAccountFromToEndpoint)
}
