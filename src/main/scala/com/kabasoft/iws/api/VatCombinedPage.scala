package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Vat}
import com.kabasoft.iws.repository.{AccountRepository, VatRepository}
import zio._
import zio.http._
import java.math.BigDecimal

object VatCombinedPage:

  private val prefix = "vat"

  private def blank(company: String): Vat =
    Vat(
      id               = "",
      name             = "",
      description      = "",
      percent          = BigDecimal("0.00"),
      inputVatAccount  = "-1",
      outputVatAccount = "-1",
      company          = company
    )

  private val pageGet: Route[VatRepository & AccountRepository, Response] =
    Method.GET / "vat-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        (for {
          all  <- VatRepository.all((ModelId.VAT.modelid, company))
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
        } yield {
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          val formHtml = VatFormView.render(blank(company), accs, mode = "view")
          val listHtml = VatListView.render(
            page, company, "",
            rowMode  = HtmxTable.RowMode.Fetch("#vat-form"),
            detailFn = v => s"/html/vat/${v.id}/${v.modelid}/${v.company}"
          )

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.VAT.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">VAT</h2>
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

          HtmxResponse.html(HtmxPage.renderRaw("VAT", bodyHtml))
        }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[VatRepository & AccountRepository, Response] = Routes(pageGet)