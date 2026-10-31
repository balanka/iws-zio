package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Company, ModelId}
import com.kabasoft.iws.domain.AppError.RepositoryError
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

  private type Repos = CompanyRepository & AccountRepository & MasterfileRepository

  private def buildBody(company: String): ZIO[Repos, RepositoryError, String] =
    for {
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

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.COMPANY.modelid, company)}
         |
         |    <div id="$prefix-form-card" class="daisy-card bg-base-100 shadow-sm">
         |      <div class="daisy-card-body compact">
         |        <div class="daisy-card-content">
         |          $formHtml
         |        </div>
         |      </div>
         |    </div>
         |
         |    <div id="$prefix-list-card" class="daisy-card bg-base-100 shadow-sm">
         |      <div class="daisy-card-body compact">
         |        <div class="daisy-card-content">$listHtml</div>
         |      </div>
         |    </div>
         |
         |  </div>
         |</div>
         |""".stripMargin
    }

  private def render(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Company", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "company-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[Repos, Response] =
    Method.GET / "html" / "company-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet, fragmentGet)