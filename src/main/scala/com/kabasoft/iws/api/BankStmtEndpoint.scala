package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.bankStatementsSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.BankStatement
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.model.Status


object BankStmtEndpoint {

  private val createAPI         = Endpoint.post("bs").in[BankStatement].out[Int].outError[RepositoryError](Status.InternalServerError)
  private val allAPI         = Endpoint.get("bs"/ string("company")).out[List[BankStatement]].outError[RepositoryError](Status.InternalServerError)
  private val byIdAPI        = Endpoint.get("bs" / string("id")/ string("company")).out[BankStatement].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI      = Endpoint.get("bs" / string("id")).out[Int].outError[RepositoryError](Status.InternalServerError)

   val createBanktmtEndpoint    = createAPI.implement(bs => BankStatementRepository.create(List(bs)).mapError(e => RepositoryError(e.getMessage)))
  private val allEndpoint    = allAPI.implement(company => BankStatementRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  private val byIdEndpoint   = byIdAPI.implement(p => BankStatementRepository.getBy(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
  private val deleteEndpoint = deleteAPI.implement(id => BankStatementRepository.delete(id, "1000").mapError(e => RepositoryError(e.getMessage)))

   val routesBankStmt = allEndpoint ++ byIdEndpoint ++ createBanktmtEndpoint ++deleteEndpoint

  val appBankStmt = routesBankStmt//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
