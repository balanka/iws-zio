package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError._
import com.kabasoft.iws.domain.{Room, AppError}
import com.kabasoft.iws.repository.RoomRepository
import com.kabasoft.iws.repository.Schema.{roomSchema, authenticationErrorSchema, repositoryErrorSchema}
import zio._
import zio.http._
import zio.http.codec._
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object RoomEndpoint:
  val modelidDoc = "The modelId for identifying the typ of room (i.e. room)"
  val idDoc = "The unique Id for identifying the  room"
  val mCreateAPIFoc="Create a new room"
  val mAllAPIDoc = "Get a room by modelId and company"
  val companyDoc = "The company whom the room belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get room by Id and modelId"
  val mModifyAPIDoc = "Modify a room"
  val mDeleteAPIDoc = "Delete a  room"

  private val mCreate = Endpoint(RoutePattern.POST / "room")
    .in[Room]
    .header(HeaderCodec.authorization)
    .out[Room]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "room" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Room]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "room" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Room] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "room").header(HeaderCodec.authorization)
    .in[Room]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Room] ?? Doc.p(mModifyAPIDoc)
  
  private val mDelete = Endpoint(RoutePattern.DELETE / "room" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val masterfileCreateRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert apartment  ${m}")
        *> RoomRepository.create(m)
        *> RoomRepository.getById(m.id, m.modelid, m.company)

  val masterfileAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"get all apartment with modelId ${p._2}   ${p}") *>
        RoomRepository.all((p._1, p._2))

  val masterfileByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify apartment  ${p}") *>
        RoomRepository.getById(p._1, p._2, p._3)

  val masterfileModifyRoute =
    mModify.implement: (_, m) =>
      ZIO.logInfo (s"Modify apartment  ${m}") *>
        RoomRepository.modify (m) *>
        RoomRepository.getById ((m.id, m.modelid, m.company) )

  val masterfileDeleteRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      RoomRepository.delete((id, modelid, company))

  val roomRoutes = Routes(masterfileCreateRoute, masterfileAllRoute, masterfileByIdRoute, masterfileModifyRoute
    , masterfileDeleteRoute) @@ Middleware.debug