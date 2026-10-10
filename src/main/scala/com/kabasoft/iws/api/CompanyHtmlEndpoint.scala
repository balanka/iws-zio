package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Company, ModelId}
import com.kabasoft.iws.repository.{
  AccountRepository, CompanyRepository, MasterfileRepository
}
import zio._
import zio.http._

object CompanyHtmlEndpoint:

  private val prefix = "company"

  private def blank(company: String): Company =
    Company(
      id = "", name = "", street = "", zip = "", city = "", state = "",
      country = "", email = "", contact = "", phone = "", bankAcc = "",
      description = "", taxCode = "-1", vatCode = "-1", currency = "EUR",
      locale = "en-US", account = "-1", oaccount = "-1",
      balanceSheetAcc = "-1", incomeStmtAcc = "-1",
      purchasingClearingAcc = "-1", salesClearingAcc = "-1", cashAcc = "-1")

  /** Build the entity from form params, over an existing / blank base. */
  private def applyParams(base: Company, params: Map[String, String]): Company =
    base.copy(
      id                    = params.getOrElse("id_display", base.id),
      name                  = params.getOrElse("name",          base.name),
      description           = params.getOrElse("description",   base.description),
      street                = params.getOrElse("street",        base.street),
      zip                   = params.getOrElse("zip",           base.zip),
      city                  = params.getOrElse("city",          base.city),
      state                 = params.getOrElse("state",         base.state),
      country               = params.getOrElse("country",       base.country),
      email                 = params.getOrElse("email",         base.email),
      contact               = params.getOrElse("contact",       base.contact),
      phone                 = params.getOrElse("phone",         base.phone),
      bankAcc               = params.getOrElse("bankAcc",       base.bankAcc),
      taxCode               = params.getOrElse("taxCode",       base.taxCode),
      vatCode               = params.getOrElse("vatCode",       base.vatCode),
      currency              = params.getOrElse("currency",      base.currency),
      locale                = params.getOrElse("locale",        base.locale),
      account               = params.getOrElse("account",       base.account),
      oaccount              = params.getOrElse("oaccount",      base.oaccount),
      balanceSheetAcc       = params.getOrElse("balanceSheetAcc",       base.balanceSheetAcc),
      incomeStmtAcc         = params.getOrElse("incomeStmtAcc",         base.incomeStmtAcc),
      purchasingClearingAcc = params.getOrElse("purchasingClearingAcc", base.purchasingClearingAcc),
      salesClearingAcc      = params.getOrElse("salesClearingAcc",      base.salesClearingAcc),
      cashAcc               = params.getOrElse("cashAcc",               base.cashAcc)
    )

  // ── New (blank form in create mode) ────────────────────────────────────

  private val formNew
  : Route[AccountRepository & MasterfileRepository, Response] =
    Method.GET / "html" / "company" / "new-form" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        (for {
          accs  <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          banks <- MasterfileRepository.all((ModelId.BANK.modelid, company))
        } yield HtmxResponse.html(
          CompanyFormView.render(blank(company), accs, banks, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  // ── GET by id (read-only view) ─────────────────────────────────────────

  private val formGet
  : Route[CompanyRepository & AccountRepository & MasterfileRepository, Response] =
    Method.GET / "html" / "company" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          c     <- CompanyRepository.getById((id, modelid))
          accs  <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          banks <- MasterfileRepository.all((ModelId.BANK.modelid, company))
        } yield HtmxResponse.html(
          CompanyFormView.render(c, accs, banks, mode = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  // ── PUT — create or modify ─────────────────────────────────────────────

  private val formPut
  : Route[CompanyRepository & AccountRepository & MasterfileRepository, Response] =
    Method.PUT / "html" / "company" ->
      handler { (req: Request) =>
        (for {
          body     <- req.body.asString
          params    = HtmxResponse.parseForm(body)
          mode      = params.getOrElse("mode", "view")
          company   = params.getOrElse("company", "")
          modelid   = params.get("modelid").flatMap(_.toIntOption)
            .getOrElse(ModelId.COMPANY.modelid)

          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else CompanyRepository.getById(
            (params.getOrElse("id", ""), modelid))
          toSave    = applyParams(existing, params)

          // create() or modify() always receive the FULL entity
          _        <- if mode == "create" then CompanyRepository.create(toSave)
          else CompanyRepository.modify(toSave)

          fresh    <- CompanyRepository.getById((toSave.id, modelid))
          accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          banks    <- MasterfileRepository.all((ModelId.BANK.modelid, company))
        } yield HtmxResponse.html(
          CompanyFormView.render(fresh, accs, banks, mode = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[
    CompanyRepository & AccountRepository & MasterfileRepository, Response
  ] = Routes(formNew, formGet, formPut) @@ Middleware.debug