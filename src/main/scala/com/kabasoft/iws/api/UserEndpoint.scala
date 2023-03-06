package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.Schema.userSchema
import com.kabasoft.iws.repository.Schema.repositoryErrorSchema
import com.kabasoft.iws.domain.User
import com.kabasoft.iws.repository._
import zio.http.codec.HttpCodec._
import zio.http.codec.HttpCodec.{int, string}
import zio.http.endpoint.Endpoint
import zio.http.model.Status

object UserEndpoint {

  val userCreateAPI      = Endpoint.post("user").in[User].out[Int].outError[RepositoryError](Status.InternalServerError)
  val userAllAPI         = Endpoint.get("user").out[List[User]].outError[RepositoryError](Status.InternalServerError)
  val userByIdAPI        = Endpoint.get("user" / int("id")).out[User].outError[RepositoryError](Status.InternalServerError)
  val userByUserNameAPI  = Endpoint.get("user" / string("userName")).out[User].outError[RepositoryError](Status.InternalServerError)
  private val deleteAPI  = Endpoint.get("user" / int("id")).out[Int].outError[RepositoryError](Status.InternalServerError)

  val userCreateEndpoint     = userCreateAPI.implement(user => UserRepository.create(List(user)).mapError(e => RepositoryError(e.getMessage)))
  val userAllEndpoint        = userAllAPI.implement(_ => UserRepository.all("1000").mapError(e => RepositoryError(e.getMessage)))
  val userByIdEndpoint       = userByIdAPI.implement(id => UserRepository.getById(id, "1000").mapError(e => RepositoryError(e.getMessage)))
  val userByUserNameEndpoint = userByUserNameAPI.implement(userName => UserRepository.getByUserName(userName, "1000").mapError(e => RepositoryError(e.getMessage)))
  private val deleteEndpoint = deleteAPI.implement(id => UserRepository.delete(id, "1000").mapError(e => RepositoryError(e.getMessage)))

   val routesUser = userAllEndpoint ++ userByIdEndpoint ++ userByUserNameEndpoint ++ userCreateEndpoint ++deleteEndpoint

  val appUser = routesUser//.toApp //@@ bearerAuth(jwtDecode(_).isDefined) ++ vatCreateEndpoint

}
