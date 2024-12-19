package com.kabasoft.iws.api
import com.kabasoft.iws.domain.AppError.*
import com.kabasoft.iws.domain.{AppError, BankStatement}
import com.kabasoft.iws.repository.Schema.{bankStatementsSchema, repositoryErrorSchema, transactionDetailsSchema}
import com.kabasoft.iws.repository.Schema._
import com.kabasoft.iws.repository._
import com.kabasoft.iws.service.BankStatementService
import zio.schema.Schema
import zio.*
import zio.http.*
import zio.http.codec.PathCodec.{path, int, string, long}
import zio.http.codec.*
import zio.http.endpoint.Endpoint



object BankStmtEndpoint:
  val DOUBLE_QUOTE = "\""
  val modelidDoc = "The modelId for identifying the typ of bank statement (i.e. cost center)"
  val idDoc = "The unique Id for identifying the  bank statement "
  val mCreateAPIFoc = "Create a new bank statement "
  val mAllAPIDoc = "Get a bank statement  by modelId and company"
  val companyDoc = "The company whom the bank statement  belongs to (i.e. 111111)"
  val pathDoc = "The company whom the bank statement  belongs to (i.e. 111111)"
  val headerDoc = "The company whom the bank statement  belongs to (i.e. 111111)"
  val charDoc = s"Special char used a field separator (i.e. $DOUBLE_QUOTE)"
  val extensionDoc = "The company whom the bank statement  belongs to (i.e. 111111)"
  val mByIdAPIDoc = "Get bank statement  by Id and modelId"
  val mModifyAPIDoc = "Modify a bank statement "
  val mDeleteAPIDoc = "Delete a  bank statement "
  val mPostAPIDoc = "Post a  bank statement "
  
  private val mCreate = Endpoint(RoutePattern.POST / "bs")
    .in[BankStatement]
    .header(HeaderCodec.authorization)
    .out[Int]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ) ?? Doc.p(mCreateAPIFoc)

  private val mAll = Endpoint(RoutePattern.GET / "bs" / int("modelid") ?? Doc.p(modelidDoc) / string("company") ??
    Doc.p(companyDoc)
  ).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[List[BankStatement]] ?? Doc.p(mAllAPIDoc)

  private val mById = Endpoint(RoutePattern.GET / "bs" / long("id") ?? Doc.p(idDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized),
    ).out[BankStatement] ?? Doc.p(mByIdAPIDoc)

  private val mModify = Endpoint(RoutePattern.PUT / "bs").header(HeaderCodec.authorization)
    .in[BankStatement]
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[BankStatement] ?? Doc.p(mModifyAPIDoc)

  private val bsPost     = Endpoint(RoutePattern.GET / "bspost"/string("company")?? Doc.p(companyDoc) / string("id")?? Doc.p(idDoc))
  .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
    HttpCodec.error[AuthenticationError](Status.Unauthorized)
  ).out[List[BankStatement]] ?? Doc.p(mPostAPIDoc)

  private val bsImportAPI    = Endpoint(RoutePattern.GET / "bs" / string("path")?? Doc.p(pathDoc)
    /  string("header")?? Doc.p(headerDoc)/ string("char")?? Doc.p(charDoc)
    / string("extension")?? Doc.p(extensionDoc)/string("company")?? Doc.p(companyDoc) )
    .header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mPostAPIDoc)

  private val mDelete = Endpoint(RoutePattern.DELETE / "bs" / string("id") ?? Doc.p(modelidDoc) / int("modelid") ?? Doc.p(modelidDoc)
    / string("company") ?? Doc.p(companyDoc)).header(HeaderCodec.authorization)
    .outErrors[AppError](HttpCodec.error[RepositoryError](Status.NotFound),
      HttpCodec.error[AuthenticationError](Status.Unauthorized)
    ).out[Int] ?? Doc.p(mDeleteAPIDoc)

  val createRoute =
    mCreate.implement: (m, _) =>
      ZIO.logInfo(s"Insert bank statement  ${m}") *>
        BankStatementRepository.create(m)

  val mAllRoute =
    mAll.implement: p =>
      ZIO.logInfo(s"Insert bank statement  ${p}") *>
        BankStatementRepository.all((p._1, p._2))

  val mByIdRoute =
    mById.implement: p =>
      ZIO.logInfo(s"Modify bank statement  ${p}") *>
        BankStatementRepository.getById(p._1, p._2, p._3)

  val mModifyRoute =
    mModify.implement: (h, m) =>
      ZIO.logInfo(s"Modify bank statement  ${m}") *>
        BankStatementRepository.modify(m) *>
        BankStatementRepository.getById((m.id, m.modelid, m.company))

  val importBSRoute =
    bsImportAPI.implement: (path, header, char, extension, company, h) =>
      ZIO.logInfo(s"Import bank statement header:${header} char:${char} extension: ${extension} company ${company} path: ${path.replace(".", "/")}") *>
        BankStatementService.importBankStmt(path.replace(".", "/"), header, char, extension, company )

  val bsPostBSRoute  =
    bsPost.implement: (company, id, h) =>
      ZIO.logInfo (s"Post Bank statements  by id ${id} for company ${company}") *>
      BankStatementService.post (buildIds (id), company)

  private def buildIds(id: String):List[Long] = {
    val ids = if (id.contains(",")) {
      id.split(",").map(_.toLong).toList
    } else List(id.toLong)
    ids
  }

  val mDeleteRoute =
    mDelete.implement: (id, modelid, company, _) =>
      MasterfileRepository.delete((id, modelid, company))
  
  val bankStmtRoutes = Routes(createRoute, mAllRoute, mByIdRoute, mModifyRoute, importBSRoute, bsPostBSRoute, mDeleteRoute) @@ Middleware.debug

