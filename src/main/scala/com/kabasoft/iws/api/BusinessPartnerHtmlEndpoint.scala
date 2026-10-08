package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{BusinessPartner, ModelId}
import com.kabasoft.iws.repository.{
  AccountRepository,
  CustomerRepository,
  EmployeeRepository,
  PartnerRepository,
  SupplierRepository,
  VatRepository
}
import zio._
import zio.http._

object BusinessPartnerHtmlEndpoint:

  // All three partner repositories plus the masterfile lists the form needs.
  type FormEnv = AccountRepository & VatRepository & PartnerRepository
  type Repos   = PartnerKind.Env & FormEnv

  // ── Form renderer ───────────────────────────────────────────────────────

  private def renderForm(kind: PartnerKind)(p: BusinessPartner, mode: String = "view")
  : ZIO[FormEnv, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    for {
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, p.company))
      vats     <- VatRepository.all((ModelId.VAT.modelid, p.company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, p.company))
    } yield BusinessPartnerFormView.render(p, accs, vats, contacts, kind, mode)

  // ── List handler ────────────────────────────────────────────────────────

  private def handleList(kind: PartnerKind, company: String, req: Request)
  : ZIO[PartnerKind.Env, Nothing, Response] =
    val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
    val query = req.queryParam("q").getOrElse("")
    val size  = req.queryParam("size").flatMap(_.toIntOption)
      .filter(HtmxTable.pageSizes.contains).getOrElse(20)
    val sort  = req.queryParam("sort")
      .map(HtmxTable.Page.parseSort)
      .filter(_.nonEmpty)
      .getOrElse(HtmxTable.Page.defaultSort)

    PartnerKind
      .loadAll(kind, company)
      .map { all =>
        val filtered =
          if query.isEmpty then all
          else all.filter(p =>
            p.id.contains(query) ||
              p.name.toLowerCase.contains(query.toLowerCase) ||
              p.city.toLowerCase.contains(query.toLowerCase)
          )
        val sorted = filtered.sortWith { (x, y) =>
          val cmp = sort.iterator
            .map { case (key, dir) =>
              val c = key match
                case "name"  => x.name.trim.compareTo(y.name.trim)
                case "city"  => x.city.trim.compareTo(y.city.trim)
                case "phone" => x.phone.trim.compareTo(y.phone.trim)
                case "email" => x.email.trim.compareTo(y.email.trim)
                case _       => x.id.trim.compareTo(y.id.trim)
              if dir == "desc" then -c else c
            }
            .find(_ != 0)
            .getOrElse(0)
          cmp < 0
        }
        val slice = sorted.slice(page * size, page * size + size)
        HtmxResponse.html(
          BusinessPartnerListView.render(
            HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
            company,
            query,
            kind
          )
        )
      }
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  // ── New (blank form in create mode) ─────────────────────────────────────

  private val formNew: Route[FormEnv, Response] =
    Method.GET / "html" / string("prefix") / "new" / int("modelid") / string("company") ->
      handler { (prefix: String, modelid: Int, company: String, _: Request) =>
        PartnerKind.fromPrefix(prefix) match
          case Some(kind) =>
            (for {
              accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
              vats     <- VatRepository.all((ModelId.VAT.modelid, company))
              contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
            } yield HtmxResponse.html(
              BusinessPartnerFormView.render(
                PartnerKind.blank(kind, company), accs, vats, contacts, kind,
                mode = "create")))
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case None =>
            ZIO.succeed(HtmxResponse.errorHtml(s"unknown partner kind: $prefix"))
      }

  // ── Form handlers ───────────────────────────────────────────────────────

  private def handleFormGet(kind: PartnerKind)(id: String, modelid: Int, company: String)
  : ZIO[Repos, Nothing, Response] =
    (for {
      p    <- PartnerKind.load(kind, id, modelid, company)
      html <- renderForm(kind)(p)              // mode = "view"
    } yield HtmxResponse.html(html))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def handleFormPut(kind: PartnerKind, req: Request)
  : ZIO[Repos, Nothing, Response] =
    (for {
      body     <- req.body.asString
      params    = HtmxResponse.parseForm(body)
      mode      = params.getOrElse("mode", "view")
      id        = params.getOrElse("id", "")
      modelid   = params.get("modelid").flatMap(_.toIntOption).getOrElse(kind.modelid)
      company   = params.getOrElse("company", "")
      existing <- if mode == "create" then ZIO.succeed(PartnerKind.blank(kind, company))
      else PartnerKind.load(kind, id, modelid, company)
      toSave    = PartnerKind.applyUpdate(existing, kind, params)
      _        <- if mode == "create" then PartnerKind.create(kind, toSave)
      else PartnerKind.save(toSave)
      fresh    <- PartnerKind.load(kind, toSave.id, modelid, company)
      html     <- renderForm(kind)(fresh)      // back to mode = "view"
    } yield HtmxResponse.html(html))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  // ── Routes ──────────────────────────────────────────────────────────────

  private val customerList: Route[PartnerKind.Env, Response] =
    Method.GET / "html" / "customer-list" / string("company") ->
      handler { (company: String, req: Request) => handleList(PartnerKind.Customer, company, req) }

  private val supplierList: Route[PartnerKind.Env, Response] =
    Method.GET / "html" / "supplier-list" / string("company") ->
      handler { (company: String, req: Request) => handleList(PartnerKind.Supplier, company, req) }

  private val employeeList: Route[PartnerKind.Env, Response] =
    Method.GET / "html" / "employee-list" / string("company") ->
      handler { (company: String, req: Request) => handleList(PartnerKind.Employee, company, req) }

  private val customerGet: Route[Repos, Response] =
    Method.GET / "html" / "customer" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        handleFormGet(PartnerKind.Customer)(id, modelid, company) }

  private val supplierGet: Route[Repos, Response] =
    Method.GET / "html" / "supplier" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        handleFormGet(PartnerKind.Supplier)(id, modelid, company) }

  private val employeeGet: Route[Repos, Response] =
    Method.GET / "html" / "employee" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        handleFormGet(PartnerKind.Employee)(id, modelid, company) }

  private val customerPut: Route[Repos, Response] =
    Method.PUT / "html" / "customer" ->
      handler { (req: Request) => handleFormPut(PartnerKind.Customer, req) }

  private val supplierPut: Route[Repos, Response] =
    Method.PUT / "html" / "supplier" ->
      handler { (req: Request) => handleFormPut(PartnerKind.Supplier, req) }

  private val employeePut: Route[Repos, Response] =
    Method.PUT / "html" / "employee" ->
      handler { (req: Request) => handleFormPut(PartnerKind.Employee, req) }

  // ── Assembly ────────────────────────────────────────────────────────────

  val routes: Routes[Repos, Response] =
    Routes(
      customerList, supplierList, employeeList,
      formNew,
      customerGet,  supplierGet,  employeeGet,
      customerPut,  supplierPut,  employeePut
    ) @@ Middleware.debug