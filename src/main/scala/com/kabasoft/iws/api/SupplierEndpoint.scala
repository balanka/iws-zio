package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, Supplier}
import com.kabasoft.iws.repository.Schema.{authenticationErrorSchema, repositoryErrorSchema, supplierschema}
import com.kabasoft.iws.repository.SupplierRepository
import zio.*
import zio.http.*
import zio.http.codec.*
import zio.http.codec.PathCodec.{int, path, string}
import zio.http.endpoint.Endpoint
import zio.schema.Schema


object SupplierEndpoint:
  val modelidDoc = "The modelId for identifying the typ of supplier "
  val idDoc = "The unique Id for identifying the  supplier"
  val mCreateAPIFoc="Create a new supplier"
  val mAllAPIDoc = "Get a supplier by modelId and company"
  val companyDoc = "The company whom the supplier belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get supplier by Id and modelId"
  val mModifyAPIDoc = "Modify a supplier"
  val mDeleteAPIDoc = "Delete a  supplier"

  private val mCreate = Endpoint(RoutePattern.POST / "sup")
    .in[Supplier]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    )?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "sup" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[Supplier]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "sup" / string("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[Supplier] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "sup").header(HeaderCodec.authorization)
    .in[Supplier]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Supplier] ?? Doc.p(mModifyAPIDoc)
  private val mDelete = Endpoint(RoutePattern.DELETE / "sup" / string("id") ?? Doc.p(modelidDoc) /int("modelid")?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createSupplierRoute =
    mCreate.implement: (m,_) =>
      ZIO.logInfo(s"Insert supplier  ${m}") *>
        SupplierRepository.create(m)

  val supplierAllRoute =
    mAll.implement : p =>
      ZIO.logInfo(s"Insert supplier  ${p}") *>
        SupplierRepository.all((p._1, p._2))

  val supplierByIdRoute =
    mById.implement: p =>
      ZIO.logInfo (s"Modify supplier  ${p}") *>
        SupplierRepository.getById(p._1, p._2, p._3)

  val modifySupplierRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo (s"Modify supplier  ${m}") *>
        SupplierRepository.modify (m) *>
        SupplierRepository.getById ((m.id, m.modelid, m.company) )

  val deleteSupplierRoute =
    mDelete.implement: (id, modelid, company, _)  =>
      SupplierRepository.delete((id, modelid, company))
  
  val supplierRoutes = Routes(createSupplierRoute, supplierAllRoute, supplierByIdRoute, modifySupplierRoute, deleteSupplierRoute) @@ Middleware.debug
