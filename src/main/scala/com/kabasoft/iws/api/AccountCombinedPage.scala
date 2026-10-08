package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, ModelId}
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

  private val pageGet: Route[AccountRepository, Response] =
    Method.GET / "account-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        AccountRepository
          .all((ModelId.ACCOUNT.modelid, company))
          .map { all =>
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

            val bodyHtml =
              s"""
                 |<div class="container mx-auto max-w-5xl">
                 |  <div class="htmx-page-wrapper">
                 |
                 |       ${HtmxToolbar.render(prefix, ModelId.ACCOUNT.modelid, company)}
                 |
                 |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
                 |      <div class="card-body compact">
                 |        <div class="card-header-row">
                 |          <h2 class="card-title">Account</h2>
                 |          <button type="button"
                 |                  class="btn btn-ghost btn-xs"
                 |                  data-toggle="#$prefix-form-card"
                 |                  data-label-show="Show"
                 |                  data-label-hide="Hide">
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

//            val bodyHtml =
//              s"""
//                 |<div class="container mx-auto max-w-5xl p-6 space-y-4">
//                 |
//                 |  ${HtmxToolbar.render(prefix)}
//                 |
//                 |  <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
//                 |    <div class="card-body">
//                 |      <h2 class="card-title mb-4">Account</h2>
//                 |      <div class="card-content">
//                 |        <div id="$prefix-form">$formHtml</div>
//                 |      </div>
//                 |    </div>
//                 |  </div>
//                 |
//                 |  <div id="$prefix-list-card" class="card bg-base-100 shadow-sm">
//                 |    <div class="card-body">
//                 |      <div class="card-content">
//                 |        $listHtml
//                 |      </div>
//                 |    </div>
//                 |  </div>
//                 |
//                 |</div>
//                 |""".stripMargin

            HtmxResponse.html(HtmxPage.renderRaw("Account", bodyHtml))
          }
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[AccountRepository, Response] = Routes(pageGet)