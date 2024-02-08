package com.kabasoft.iws.api

import com.kabasoft.iws.repository.Schema.{accountSchema, repositoryErrorSchema}
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

  val accCreateAPI = Endpoint.post("acc").in[Account].out[Account].outError[RepositoryError](Status.InternalServerError)
  val accAllAPI = Endpoint.get("acc"/ int("modelid")/ string("company")).out[List[Account]].outError[RepositoryError](Status.InternalServerError)
  val balanceAPI = Endpoint.get("balance" / string("company")/string("accId") / int("to")).out[List[Account]]
    .outError[RepositoryError](Status.InternalServerError)
  val accByIdAPI = Endpoint.get("acc" / string("id")/ string("company")).out[Account].outError[RepositoryError](Status.InternalServerError)

  val accModifyAPI     = Endpoint.put("acc").in[Account].out[Account].outError[RepositoryError](Status.InternalServerError)
  val deleteAPI = Endpoint.delete("acc" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
  val closePeriodAPI = Endpoint.get("close" / string("accId") / int("to")/ string("company")).out[Int]
    .outError[RepositoryError](Status.InternalServerError)


  private val accAllEndpoint = accAllAPI.implement(Id => AccountCache.all(Id).mapError(e => RepositoryError(e.getMessage)))
  val accCreateEndpoint = accCreateAPI.implement(account =>
    ZIO.logInfo(s"Insert Account  ${account}") *>
    AccountRepository.create(account).mapError(e => RepositoryError(e.getMessage)))
  val balanceEndpoint = balanceAPI.implement { case (company:String, accId: String, to: Int) =>
    ZIO.logInfo(s"get balance  period at ${to}  ${accId}") *>
    AccountService.getBalance(accId,  to, company).mapError(e => RepositoryError(e.getMessage))}
  val accByIdEndpoint = accByIdAPI.implement (p => AccountCache.getBy(p).mapError(e => RepositoryError(e.getMessage)))
  val closePeriodEndpoint = closePeriodAPI.implement { case (accId: String,  to: Int, company:String) =>
    ZIO.logInfo(s"closing period at  ${to}  ${accId}") *>
    AccountService. closePeriod(to, accId, company).mapError(e => RepositoryError(e.getMessage))}
  val accModifyEndpoint = accModifyAPI.implement(p => ZIO.logInfo(s"Modify account  ${p}") *>
    AccountRepository.modify(p).mapError(e => RepositoryError(e.getMessage))*>
    AccountRepository.getBy((p.id, p.company)).mapError(e => RepositoryError(e.getMessage)))
  val accDeleteEndpoint = deleteAPI.implement { case (id,company) => AccountRepository.delete(id, company).mapError(e => RepositoryError(e.getMessage))}

  val routes = accAllEndpoint ++ accByIdEndpoint ++ balanceEndpoint ++ closePeriodEndpoint ++ accCreateEndpoint ++ accDeleteEndpoint++accModifyEndpoint

  val appAcc = routes //.toApp @@ bearerAuth(jwtDecode(_).isDefined)

}
