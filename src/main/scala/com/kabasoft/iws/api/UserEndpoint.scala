package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.{AuthenticationError, RepositoryError}
import com.kabasoft.iws.domain.{AppError, User}
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, userSchema}
import com.kabasoft.iws.repository.UserRepository
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint

object UserEndpoint:
  val modelidDoc = "The modelId for identifying the typ of user"
  val idDoc = "The unique Id for identifying the user"
  val mCreateAPIFoc = "Create a new User"
  val mAllAPIDoc = "Get a user by modelId and company"
  val companyDoc = "The company whom the user belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get user by Id and modelId"
  val mByNameAPIDoc = "Get user by user name and modelId"
  val mModifyAPIDoc = "Modify a user"
  val mModifyUserPwdDoc = "Modify a user's password"
  val mDeleteAPIDoc = "Delete a user"

  private val userCreate = Endpoint(RoutePattern.POST / "user")
    .in[User]
    .header(HeaderCodec.authorization)
    .out[User]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val userAll = Endpoint(RoutePattern.GET / "user" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[User]] ?? Doc.p(mAllAPIDoc)

  private val userById = Endpoint(RoutePattern.GET / "user" / int("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[User] ?? Doc.p(mByIdAPIDoc)

  private val userByName = Endpoint(RoutePattern.GET / "user" / string("userName") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[User] ?? Doc.p(mByNameAPIDoc)

  private val userModify = Endpoint(RoutePattern.PUT / "user").header(HeaderCodec.authorization)
    .in[User]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[User] ?? Doc.p(mModifyAPIDoc)

  private val userModifyPwd = Endpoint(RoutePattern.PUT / "userpwd").header(HeaderCodec.authorization)
    .in[User]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[User] ?? Doc.p(mModifyUserPwdDoc)
  
  private val userDelete = Endpoint(RoutePattern.DELETE / "user" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createUserRoute =
    userCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert user  ${m}") 
        *> UserRepository.create(m)
        *> UserRepository.getById(m.id, m.modelid, m.company)

  val allUserRoute =
    userAll.implement: p =>
      ZIO.logInfo(s"Insert user  ${p}") *>
        UserRepository.all((p._1, p._2))

  val userByIdRoute =
    userById.implement: p =>
      ZIO.logInfo(s"Get  user by id  ${p}") *>
        UserRepository.getById(p._1, p._2, p._3)
      
  val userByNameRoute =
    userByName.implement: p =>
      ZIO.logInfo(s"Get user by name   ${p}") *>
        UserRepository.getByUserName(p._1, p._2, p._3)

  val modifyUserRoute =
    userModify.implement: (_, m) =>
      ZIO.logInfo(s"Modify user  ${m}") *>
        UserRepository.modify(m) *>
        UserRepository.getByUserName(m.userName, m.modelid, m.company)
      
  val modifyUserPwdRoute =
    userModifyPwd.implement: (_, m) =>
      ZIO.logInfo(s"Modify user's pwd  ${m}") *>
        UserRepository.modifyPwd(m) *>
        UserRepository.getByUserName(m.userName, m.modelid, m.company)

  val deleteUserRoute =
    userDelete.implement: (id, modelid, company, _) => 
      UserRepository.delete((id, modelid, company))


  val userRoutes = Routes(createUserRoute, allUserRoute, userByIdRoute, userByNameRoute,
                           modifyUserRoute, modifyUserPwdRoute, deleteUserRoute) @@ Middleware.debug
  
//  val userCreateAPI      = Endpoint.post("user").in[User].out[User].outError[RepositoryError](Status.InternalServerError)
//  val userAllAPI         = Endpoint.get("user"/ int("modelid")/string("company")).out[List[User]].outError[RepositoryError](Status.InternalServerError)
// // val userByIdAPI        = Endpoint.get("user" / int("id")).out[User].outError[RepositoryError](Status.InternalServerError)
// val userModifyAPI     = Endpoint.put("user").in[User].out[User].outError[RepositoryError](Status.InternalServerError)
//  val userModifyPwdAPI     = Endpoint.put("userpwd").in[User].out[User].outError[RepositoryError](Status.InternalServerError)
//  val userByUserNameAPI  = Endpoint.get("user" / string("userName")/ string("company")).out[User].outError[RepositoryError](Status.InternalServerError)
//  private val deleteAPI  = Endpoint.delete("user" / string("userName")/ int("modelid")/string("company")).out[Int].outError[RepositoryError](Status.InternalServerError)
//
//  val userCreateEndpoint     = userCreateAPI.implement(user => UserRepository.create(user, true).mapError(e => RepositoryError(e.getMessage)))
//  val userAllEndpoint        = userAllAPI.implement(p => UserRepository.all(p).mapError(e => RepositoryError(e.getMessage)))
//  //val userByIdEndpoint       = userByIdAPI.implement(id => UserRepository.getById(id, "1000").mapError(e => RepositoryError(e.getMessage)))
//  val userModifyEndpoint = userModifyAPI.implement(p => ZIO.logInfo(s"Modify user  ${p}") *>
//    UserRepository.modify(p).mapError(e => RepositoryError(e.getMessage)) *>
//    UserRepository.getById(p.userName, p.modelid, p.company).mapError(e => RepositoryError(e.getMessage)))
//  val userModifyPwdEndpoint = userModifyPwdAPI.implement(p => ZIO.logInfo(s"Modify pwd  ${p}") *>
//    UserRepository.modifyPwd(p).mapError(e => RepositoryError(e.getMessage)) *>
//    UserRepository.getById(p.userName, p.modelid, p.company).mapError(e => RepositoryError(e.getMessage)))
//  val userByUserNameEndpoint = userByUserNameAPI.implement(p => UserRepository.getByUserName(p._1, User.MODELID, p._2).mapError(e => RepositoryError(e.getMessage)))
//   val userDeleteEndpoint = deleteAPI.implement(p => UserRepository.delete(p).mapError(e => RepositoryError(e.getMessage)))
//
//   val routesUser = userAllEndpoint  ++ userByUserNameEndpoint ++ userCreateEndpoint ++userDeleteEndpoint++
//     userModifyEndpoint++userModifyPwdEndpoint
//
//  val appUser = routesUser


