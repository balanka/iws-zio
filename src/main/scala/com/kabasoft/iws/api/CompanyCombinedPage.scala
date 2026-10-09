package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Company, ModelId}
import com.kabasoft.iws.repository.{
  AccountRepository,
  CompanyRepository,
  MasterfileRepository
}
import zio._
import zio.http._

object CompanyCombinedPage:

  private val prefix = "company"

  private def blank(company: String): Company =
    Company(
      id = "", name = "", street = "", zip = "", city = "", state = "",
      country = "", email = "", contact = "", phone = "", bankAcc = "",
      description = "", taxCode = "-1", vatCode = "-1", currency = "EUR",
      locale = "en-US", account = "-1", oaccount = "-1",
      balanceSheetAcc = "-1", incomeStmtAcc = "-1",
      purchasingClearingAcc = "-1", salesClearingAcc = "-1", cashAcc = "-1")

  private val pageGet: Route[CompanyRepository & AccountRepository & MasterfileRepository, Response] =
    Method.GET / "company-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        (for {
          all   <- CompanyRepository.all((ModelId.COMPANY.modelid))
          accs  <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          banks <- MasterfileRepository.all((ModelId.BANK.modelid, company))
        } yield {
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          val formHtml = CompanyFormView.render(blank(company), accs, banks, mode = "view")
          val listHtml = CompanyListView.render(page, company, "")

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.COMPANY.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">Company</h2>
               |          <button type="button" class="btn btn-ghost btn-xs"
               |                  data-toggle="#$prefix-form-card"
               |                  data-label-show="Show" data-label-hide="Hide">
               |            <span class="toggle-arrow">▾</span>
               |            <span class="toggle-label">Hide</span>
               |          </button>
               |        </div>
               |        <div class="card-content">
               |          <div id="$prefix-form">$formHtml</div>
               |        </div>
               |      </div>
               |    </div>
               |
               |    <div id="$prefix-list-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-content">$listHtml</div>
               |      </div>
               |    </div>
               |
               |  </div>
               |</div>
               |""".stripMargin

          HtmxResponse.html(HtmxPage.renderRaw("Company", bodyHtml))
        }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[CompanyRepository & AccountRepository & MasterfileRepository, Response] =
    Routes(pageGet)