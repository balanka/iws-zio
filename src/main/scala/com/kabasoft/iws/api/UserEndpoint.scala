package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.userSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.User
import com.kabasoft.iws.repository._
import zio.ZIO
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.{int, string}
import zio.http.endpoint.Endpoint
import zio.http.Status

object UserEndpoint {

  val userCreateAPI      = Endpoint.post("user").in[User].out[Int].outError[RepositoryError](Status.InternalServerError)
  val userAllAPI         = Endpoint.get("user"/ string("company")).out[List[User]].outError[RepositoryError](Status.InternalServerError)
 // val userByIdAPI        = Endpoint.get("user" / int("id")).out[User].outError[RepositoryError](Status.InternalServerError)
 val userModifyAPI     = Endpoint.put("user").in[User].out[User].outError[RepositoryError](Status.InternalServerError)
  val userByUserNameAPI  = Endpoint.get("user" / string("userName")/ string("company")).out[User].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI  = Endpoint.delete("user" / int("id")/ string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val userCreateEndpoint     = userCreateAPI.implement(user => UserRepository.create(List(user)).mapError(e => RepositoryError(e.getMessage)))
  val userAllEndpoint        = userAllAPI.implement(company => UserRepository.all(company).mapError(e => RepositoryError(e.getMessage)))
  //val userByIdEndpoint       = userByIdAPI.implement(id => UserRepository.getById(id, "1000").mapError(e => RepositoryError(e.getMessage)))
  val userModifyEndpoint = userModifyAPI.implement(p => ZIO.logInfo(s"Modify user  ${p}") *>
    UserRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
    UserRepository.getById(p.id, p.company).mapError(e => RepositoryError(e.getMessage)))
  val userByUserNameEndpoint = userByUserNameAPI.implement(p => UserRepository.getByUserName(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))
   val userDeleteEndpoint = deleteAPI.implement(p => UserRepository.delete(p._1, p._2).mapError(e => RepositoryError(e.getMessage)))

   val routesUser = userAllEndpoint  ++ userByUserNameEndpoint ++ userCreateEndpoint ++userDeleteEndpoint++userModifyEndpoint

  val appUser = routesUser//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ vatCreateEndpoint

}
