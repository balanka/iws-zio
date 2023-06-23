package com.kabasoft.iws.api

import com.kabasoft.iws.repository.Schema.accountSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.service.AccountService
import com.kabasoft.iws.repository.{AccountCache, AccountRepository}
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.Status


object AccountEndpoint {

  val accCreateAPI = Endpoint.post("acc").in[Account].out[Int].outError[RepositoryError](Status.InternalServerError)
  val accAllAPI = Endpoint.get("acc"/ string("company")).out[List[Account]].outError[RepositoryError](Status.InternalServerError)
  val balanceAPI = Endpoint.get("balance" / string("company")/string("accId") / int("from") / int("to")).out[List[Account]]
    .outError[RepositoryError](Status.InternalServerError)
  val accByIdAPI = Endpoint.get("acc" / string("id")/ string("company")).out[Account].outError[RepositoryError](Status.InternalServerError)
  val accModifyAPI     = Endpoint.put("acc").in[Account].out[Int].outError[RepositoryError](Status.InternalServerError)
  val deleteAPI = Endpoint.delete("acc" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
  val closePeriodAPI = Endpoint.get("balance" / string("accId") / int("from") / int("to")/ string("company")).out[Int]
    .outError[RepositoryError](Status.InternalServerError)


  private val accAllEndpoint = accAllAPI.implement(company => AccountCache.all(company).mapError(e => RepositoryError(e.getMessage)))
  val accCreateEndpoint = accCreateAPI.implement(account =>
    ZIO.logDebug(s"Insert Account  ${account}") *>
    AccountRepository.create(List(account)).mapError(e => RepositoryError(e.getMessage)))
  val balanceEndpoint = balanceAPI.implement { case (company:String, accId: String, from: Int, to: Int) =>
    AccountService.getBalance(accId, from, to, company).mapError(e => RepositoryError(e.getMessage))}
  val accByIdEndpoint = accByIdAPI.implement (p => AccountCache.getBy(p).mapError(e => RepositoryError(e.getMessage))
  val closePeriodEndpoint = closePeriodAPI.implement { case (accId: String, from: Int, to: Int, company:String) =>
    AccountService.closePeriod(from, to, accId, company).mapError(e => RepositoryError(e.getMessage))}
  val accModifyEndpoint = accModifyAPI.implement(p => ZIO.logInfo(s"Modify account  ${p}") *>
    AccountRepository.modify(p).mapError(e => RepositoryError(e.getMessage)))
  val accDeleteEndpoint = deleteAPI.implement { case (id,company) => AccountRepository.delete(id, company).mapError(e => RepositoryError(e.getMessage))}

  val routes = accAllEndpoint ++ accByIdEndpoint ++ balanceEndpoint ++ closePeriodEndpoint ++ accCreateEndpoint ++ accDeleteEndpoint++accModifyEndpoint

  val appAcc = routes //.toApp @@ bearerAuth(jwtDecode(_).isDefined)

}
