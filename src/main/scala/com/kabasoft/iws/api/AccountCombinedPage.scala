package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, ModelId}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.AccountRepository
import zio._
import zio.http._

object AccountCombinedPage:

  private val prefix = "account"

  private def blank(company: String): Account =
    Account(
      id           = "",
      name         = "",
      description  = "",
      company      = company,
      account      = "-1",
      isDebit      = false,
      balancesheet = false,
      currency     = ""
    )

  private def buildBody(company: String): ZIO[AccountRepository, RepositoryError, String] =
    AccountRepository.all((ModelId.ACCOUNT.modelid, company)).map { all =>
      val sorted      = all.sortBy(_.id)
      val initialSize = 20
      val slice       = sorted.take(initialSize)
      val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

      val formHtml = AccountFormView.render(blank(company), all)
      val listHtml = AccountListView.render(
        page, company, "",
        rowMode  = HtmxTable.RowMode.Fetch("#account-form"),
        detailFn = a => s"/html/account/${a.id}/${a.modelid}/${a.company}"
      )

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.ACCOUNT.modelid, company)}
         |
         |    <div id="$prefix-form-card" class="daisy-card bg-base-100 shadow-sm">
         |      <div class="daisy-card-body compact">
         |        <div class="daisy-card-header-row">
         |          <h2 class="daisy-card-title" id="$prefix-card-title">Account</h2>
         |          <button type="button"
         |                  class="daisy-btn btn-ghost daisy-btn-xs"
         |                  data-toggle="#$prefix-form-card"
         |                  data-label-show="Show"
         |                  data-label-hide="Hide">
         |            <span class="toggle-arrow">▾</span>
         |            <span class="toggle-daisy-label">Hide</span>
         |          </button>
         |        </div>
         |        <div class="daisy-card-content">
         |          <div id="$prefix-form">$formHtml</div>
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

  private def render(company: String): ZIO[AccountRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Account", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[AccountRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[AccountRepository, Response] =
    Method.GET / "account-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[AccountRepository, Response] =
    Method.GET / "html" / "account-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[AccountRepository, Response] = Routes(pageGet, fragmentGet)