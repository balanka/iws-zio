package com.kabasoft.iws.api

import com.kabasoft.iws.repository.Schema.accountSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.service.AccountService
import com.kabasoft.iws.repository.AccountRepository
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status


object AccountEndpoint {

  val accCreateAPI = Endpoint.post("acc").in[Account].out[Int].outError[RepositoryError](Status.InternalServerError)
  val accAllAPI = Endpoint.get("acc"/ string("company")).out[List[Account]].outError[RepositoryError](Status.InternalServerError)
  val balanceAPI = Endpoint.get("balance" / string("accId") / int("from") / int("to")).out[List[Account]]
    .outError[RepositoryError](Status.InternalServerError)
  val accByIdAPI = Endpoint.get("acc" / string("id")).out[Account].outError[RepositoryError](Status.InternalServerError)
  val deleteAPI = Endpoint.get("acc" / string("id")).out[Int].outError[RepositoryError](Status.InternalServerError)
  val closePeriodAPI = Endpoint.get("balance" / string("accId") / int("from") / int("to")).out[Int]
    .outError[RepositoryError](Status.InternalServerError)


  val accAllEndpoint = accAllAPI.implement(company => AccountRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  val accCreteEndpoint = accCreateAPI.implement(account => AccountRepository.create(List(account)).mapError(e => RepositoryError(e.getMessage)))
  val balanceEndpoint = balanceAPI.implement { case (accId: String, from: Int, to: Int) =>
    AccountService.getBalance(accId, from, to, "1000").mapError(e => RepositoryError(e.getMessage))}
  val accByIdEndpoint = accByIdAPI.implement(id => AccountRepository.getBy(id, "1000").mapError(e => RepositoryError(e.getMessage)))
  val closePeriodEndpoint = closePeriodAPI.implement { case (accId: String, from: Int, to: Int) =>
    AccountService.closePeriod(from, to, accId, "1000").mapError(e => RepositoryError(e.getMessage))}
  val deleteEndpoint = deleteAPI.implement(id => AccountRepository.delete(id, "1000").mapError(e => RepositoryError(e.getMessage)))

  val routes = accAllEndpoint ++ accByIdEndpoint ++ balanceEndpoint ++ closePeriodEndpoint ++ accCreteEndpoint ++ deleteEndpoint

  val appAcc = routes //.toApp @@ bearerAuth(jwtDecode(_).isDefined)

}
