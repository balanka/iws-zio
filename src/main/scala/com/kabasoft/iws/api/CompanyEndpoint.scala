package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.companySchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.Company
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.string
import zio.http.endpoint.Endpoint
import zio.http.Status
import zio.schema.DeriveSchema.gen

object CompanyEndpoint {

  private val companyCreateAPI      = Endpoint.post("comp").in[Company].out[Int].outError[RepositoryError](Status.InternalServerError)
  private val companyAllAPI         = Endpoint.get("comp").out[List[Company]].outError[RepositoryError](Status.InternalServerError)
  private val companyByIdAPI        = Endpoint.get("comp" / string("id")).out[Company].outError[RepositoryError](Status.InternalServerError)
  val compModifyAPI     = Endpoint.put("comp").in[Company].out[Company].outError[RepositoryError](Status.InternalServerError)
  private val companyDeleteAPI      = Endpoint.get("comp" / string("id")).out[Int].outError[RepositoryError](Status.InternalServerError)

  private val companyCreateEndpoint    = companyCreateAPI.implement(comp => CompanyRepository.create(List(comp)).mapError(e => RepositoryError(e.getMessage)))
  private val companyAllEndpoint    = companyAllAPI.implement(_ => CompanyRepository.all.mapError(e => RepositoryError(e.getMessage)))
  private val companyByIdEndpoint   = companyByIdAPI.implement(id => CompanyRepository.getBy(id).mapError(e => RepositoryError(e.getMessage)))
  val moduleModifyEndpoint = compModifyAPI.implement(p => ZIO.logInfo(s"Modify company  ${p}") *>
    CompanyRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    CompanyRepository.getBy(p.id).mapError(e => RepositoryError(e.getMessage)))
  private val companyDeleteEndpoint = companyDeleteAPI.implement(id => CompanyRepository.delete(id).mapError(e => RepositoryError(e.getMessage)))

  private val routesCompany = companyAllEndpoint ++ companyByIdEndpoint ++ companyCreateEndpoint++companyDeleteEndpoint++moduleModifyEndpoint

  val appComp= routesCompany//.toApp @@ bearerAuth(jwtDecode(_).isDefined)  //++ createEndpoint
}
