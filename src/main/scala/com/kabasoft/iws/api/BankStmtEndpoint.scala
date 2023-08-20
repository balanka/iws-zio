package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.RepositoryError
//import com.kabasoft.iws.repository.Schema.{bankStatementsSchema, repositoryErrorSchema, transactionDetailsSchema}
import com.kabasoft.iws.repository.Schema._
import com.kabasoft.iws.domain.BankStatement
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.BankStatementService
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.Status


object BankStmtEndpoint {

  private val createAPI         = Endpoint.post("bs").in[BankStatement].out[Int].outError[RepositoryError](Status.InternalServerError)
  private val allAPI         = Endpoint.get("bs"/ string("company")).out[List[BankStatement]].outError[RepositoryError](Status.InternalServerError)
  private val byIdAPI        = Endpoint.get("bs" / string("id")/ string("company")).out[BankStatement].outError[RepositoryError](Status.InternalServerError)
  val bsModifyAPI     = Endpoint.put("bs").in[BankStatement].out[BankStatement].outError[RepositoryError](Status.InternalServerError)
  private val bsPostAPI     = Endpoint.get("bs"/literal("post")/string("company")/ string("transid") ).out[List[BankStatement]].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI      = Endpoint.get("bs" / string("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

   val createBanktmtEndpoint    = createAPI.implement(bs => BankStatementRepository.create2(List(bs)).mapError(e => RepositoryError(e.getMessage)))
  private val allEndpoint    = allAPI.implement(company => BankStatementRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  private val byIdEndpoint   = byIdAPI.implement(p => BankStatementRepository.getBy(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
  val bsModifyEndpoint = bsModifyAPI.implement(p => ZIO.logInfo(s"Modify BankStatement  ${p}") *>
    BankStatementRepository.update(p).mapError(e => RepositoryError(e.getMessage)))
    //BankStatementRepository.getBy(p.id.toString, p.company).mapError(e => RepositoryError(e.getMessage)))

  private val bsDeleteEndpoint = deleteAPI.implement(p => BankStatementRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))


  private val bsPostAllEndpoint = bsPostAPI.implement(p => {
    val ids = if (p._2.contains(",")){
      p._2.split(",").map(_.toLong).toList
    } else {
      List(p._2.toLong)
    }
    ZIO.logInfo(s"Post Bank statements  by id ${ids} for company ${p._1}") *>
    BankStatementService.post(ids, p._1).mapError(e => RepositoryError(e.getMessage))})

   val routesBankStmt = allEndpoint ++ byIdEndpoint ++ createBanktmtEndpoint ++bsDeleteEndpoint++bsModifyEndpoint++bsPostAllEndpoint

  val appBankStmt = routesBankStmt//.toApp //@@ bearerAuth(jwtDecode(_).isDefined)

}
