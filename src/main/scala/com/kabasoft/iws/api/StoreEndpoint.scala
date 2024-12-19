package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Store}
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, storeSchema}
import com.kabasoft.iws.repository.StoreRepository
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema

object StoreEndpoint:
  val modelidDoc = "The modelId for identifying the typ of store "
  val idDoc = "The unique Id for identifying the store"
  val mCreateAPIFoc="Create a new store"
  val mAllAPIDoc = "Get a store by modelId and company"
  val companyDoc = "The company whom the store belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get store by Id and modelId"
  val mModifyAPIDoc = "Modify a store"
  val mDeleteAPIDoc = "Delete a store"

  private val mCreate = Endpoint(RoutePattern.POST / "store")
    .in[Store]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "store" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Store]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "store" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Store] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "store").header(HeaderCodec.authorization)
    .in[Store]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Store] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "store" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createStoreRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert store  ${m}") *>
        StoreRepository.create(m)

  val storeAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"Get all store  ${p}") *>
        StoreRepository.all((p._1, p._2))

  val storeByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify store  ${p}") *>
        StoreRepository.getById(p._1, p._2, p._3)

  val modifyStoreRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo (s"Modify store  ${m}") *>
        StoreRepository.modify (m) *>
        StoreRepository.getById ((m.id, m.modelid, m.company) )

  val deleteStoreRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      StoreRepository.delete((id, modelid, company))
  val storeRoutes = Routes(createStoreRoute, storeAllRoute, storeByIdRoute, modifyStoreRoute, deleteStoreRoute) @@ Middleware.debug

