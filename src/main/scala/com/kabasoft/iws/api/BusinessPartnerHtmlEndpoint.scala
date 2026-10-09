package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{BankAccount, BusinessPartner, Customer, Employee, ModelId, Supplier}
import com.kabasoft.iws.repository.{
  AccountRepository,
  BankAccountRepository,
  CustomerRepository,
  EmployeeRepository,
  MasterfileRepository,
  PartnerRepository,
  SupplierRepository,
  VatRepository
}
import zio._
import zio.http._

object BusinessPartnerHtmlEndpoint:

  // All three partner repositories plus the masterfile lists the form needs.
  type FormEnv = AccountRepository & VatRepository & PartnerRepository &
    BankAccountRepository & MasterfileRepository
  type Repos   = PartnerKind.Env & FormEnv

  // ── Params → entity ─────────────────────────────────────────────────────

  private def applyParams(existing: BusinessPartner, kind: PartnerKind, params: Map[String, String]): BusinessPartner =
    val parentId = params.getOrElse("id_display", existing.id)

    val name        = params.getOrElse("name",        existing.name)
    val description = params.getOrElse("description", existing.description)
    val street      = params.getOrElse("street",      existing.street)
    val zip         = params.getOrElse("zip",         existing.zip)
    val city        = params.getOrElse("city",        existing.city)
    val state       = params.getOrElse("state",       existing.state)
    val country     = params.getOrElse("country",     existing.country)
    val phone       = params.getOrElse("phone",       existing.phone)
    val email       = params.getOrElse("email",       existing.email)
    val account     = params.getOrElse("account",     existing.account)
    val oaccount    = params.getOrElse("oaccount",    existing.oaccount)
    val taxCode     = params.getOrElse("taxCode",     existing.taxCode)
    val vatCode     = params.getOrElse("vatCode",     existing.vatCode)
    val currency    = params.getOrElse("currency",    existing.currency)
    val contact     = params.getOrElse("contact",     existing.contact)

    // The owner is always the parent's id — the row's hidden `owner`
    // input carries the same value, but the server is authoritative.
    val bankAccounts: List[BankAccount] =
      HtmxFormIndex.parseIndexed(params, "bankaccounts").map { m =>
        BankAccount(
          id      = m.getOrElse("id", ""),
          bic     = m.getOrElse("bic", ""),
          owner   = parentId,
          company = existing.company,
          modelid = ModelId.BANK_ACCOUNT.modelid)
      }

    (kind, existing) match
      case (Customer, c: Customer) =>
        c.copy(id = parentId, name = name, description = description,
          street = street, zip = zip, city = city, state = state,
          country = country, phone = phone, email = email,
          account = account, oaccount = oaccount,
          taxCode = taxCode, vatCode = vatCode, currency = currency,
          contact = contact, bankaccounts = bankAccounts)

      case (Supplier, s: Supplier) =>
        s.copy(id = parentId, name = name, description = description,
          street = street, zip = zip, city = city, state = state,
          country = country, phone = phone, email = email,
          account = account, oaccount = oaccount,
          taxCode = taxCode, vatCode = vatCode, currency = currency,
          contact = contact, bankaccounts = bankAccounts)

      case (Employee, e: Employee) =>
        val salary = java.math.BigDecimal(params.getOrElse("salary", e.salary.toString))
        e.copy(id = parentId, name = name, description = description,
          street = street, zip = zip, city = city, state = state,
          country = country, phone = phone, email = email,
          account = account, oaccount = oaccount,
          taxCode = taxCode, vatCode = vatCode, currency = currency,
          contact = contact, bankaccounts = bankAccounts, salary = salary)

      case _ => existing

  // ── Form renderer ───────────────────────────────────────────────────────

  private def renderForm(kind: PartnerKind)(p: BusinessPartner, mode: String = "view")
  : ZIO[FormEnv, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    for {
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, p.company))
      vats     <- VatRepository.all((ModelId.VAT.modelid, p.company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, p.company))
      bankAccs <- BankAccountRepository.getByOwner(
        p.id,
        ModelId.BANK_ACCOUNT.modelid,
        p.company)
      bankMf   <- MasterfileRepository.all((ModelId.BANK.modelid, p.company))
      // BusinessPartner is a sealed trait — copy exists only on the
      // concrete case classes, so match and rebuild.
      withBanks = p match
        case c: Customer => c.copy(bankaccounts = bankAccs)
        case s: Supplier => s.copy(bankaccounts = bankAccs)
        case e: Employee => e.copy(bankaccounts = bankAccs)
        case _           => p
    } yield BusinessPartnerFormView.render(
      withBanks, accs, vats, contacts, bankMf, kind, mode)

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

  private val formNew
  : Route[AccountRepository & VatRepository & PartnerRepository & MasterfileRepository, Response] =
    Method.GET / "html" / string("prefix") / "new" / int("modelid") / string("company") ->
      handler { (prefix: String, modelid: Int, company: String, _: Request) =>
        PartnerKind.fromPrefix(prefix) match
          case Some(kind) =>
            (for {
              accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
              vats     <- VatRepository.all((ModelId.VAT.modelid, company))
              contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
              bankMf   <- MasterfileRepository.all((ModelId.BANK.modelid, company))
            } yield HtmxResponse.html(
              BusinessPartnerFormView.render(
                PartnerKind.blank(kind, company), accs, vats, contacts, bankMf, kind,
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