package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{BankAccount, Company, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, CompanyRepository}
import zio._
import zio.http._

object CompanyHtmlEndpoint:

  type Repos = CompanyRepository & AccountRepository

  private def blank(company: String): Company =
    Company(id = "", name = "", street = "", zip = "", city = "", state = "",
      country = "", email = "", contact = "", phone = "", bankAcc = "",
      description = "", taxCode = "-1", vatCode = "-1", currency = "EUR",
      locale = "en-US", account = "-1", oaccount = "-1",
      balanceSheetAcc = "-1", incomeStmtAcc = "-1",
      purchasingClearingAcc = "-1", salesClearingAcc = "-1", cashAcc = "-1")

  private def renderCompany(c: Company, mode: String = "view")
  : ZIO[AccountRepository, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    AccountRepository.all((ModelId.ACCOUNT.modelid, c.id))
      .map(accounts => CompanyFormView.render(c, accounts, mode))

  private def applyParams(existing: Company, params: Map[String, String]): Company =
    val parentId = params.getOrElse("id_display", existing.id)

    val bankAccounts: List[BankAccount] =
      HtmxFormIndex.parseIndexed(params, "bankaccounts").map { m =>
        BankAccount(
          id      = m.getOrElse("id", ""),
          bic     = m.getOrElse("bic", ""),
          owner   = parentId,
          company = parentId,
          modelid = ModelId.BANK_ACCOUNT.modelid)
      }

    existing.copy(
      id                    = parentId,
      name                  = params.getOrElse("name", existing.name),
      description           = params.getOrElse("description", existing.description),
      street                = params.getOrElse("street", existing.street),
      zip                   = params.getOrElse("zip", existing.zip),
      city                  = params.getOrElse("city", existing.city),
      state                 = params.getOrElse("state", existing.state),
      country               = params.getOrElse("country", existing.country),
      email                 = params.getOrElse("email", existing.email),
      contact               = params.getOrElse("contact", existing.contact),
      phone                 = params.getOrElse("phone", existing.phone),
      bankAcc               = params.getOrElse("bankAcc", existing.bankAcc),
      taxCode               = params.getOrElse("taxCode", existing.taxCode),
      vatCode               = params.getOrElse("vatCode", existing.vatCode),
      currency              = params.getOrElse("currency", existing.currency),
      locale                = params.getOrElse("locale", existing.locale),
      account               = params.getOrElse("account", existing.account),
      oaccount              = params.getOrElse("oaccount", existing.oaccount),
      balanceSheetAcc       = params.getOrElse("balanceSheetAcc", existing.balanceSheetAcc),
      incomeStmtAcc         = params.getOrElse("incomeStmtAcc", existing.incomeStmtAcc),
      purchasingClearingAcc = params.getOrElse("purchasingClearingAcc", existing.purchasingClearingAcc),
      salesClearingAcc      = params.getOrElse("salesClearingAcc", existing.salesClearingAcc),
      cashAcc               = params.getOrElse("cashAcc", existing.cashAcc),
      bankaccounts          = bankAccounts)

  private val listGet: Route[CompanyRepository, Response] =
    Method.GET / "html" / "company-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)
        CompanyRepository.all((ModelId.COMPANY.modelid)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(c => c.id.contains(query) ||
              c.name.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "name"    => x.name.trim.compareTo(y.name.trim)
                case "city"    => x.city.trim.compareTo(y.city.trim)
                case "country" => x.country.trim.compareTo(y.country.trim)
                case "email"   => x.email.trim.compareTo(y.email.trim)
                case _         => x.id.trim.compareTo(y.id.trim)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            CompanyListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[AccountRepository, Response] =
    Method.GET / "html" / "company" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          .map(accs => HtmxResponse.html(
            CompanyFormView.render(blank(company), accs, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "company" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          c    <- CompanyRepository.getById((id, modelid))
          html <- renderCompany(c)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "company" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.getOrElse("id", "")
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(ModelId.COMPANY.modelid)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else CompanyRepository.getById((id, modelid))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then CompanyRepository.create(toSave).unit
          else CompanyRepository.modify(toSave).unit
          fresh    <- CompanyRepository.getById((toSave.id, modelid))
          html     <- renderCompany(fresh)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug