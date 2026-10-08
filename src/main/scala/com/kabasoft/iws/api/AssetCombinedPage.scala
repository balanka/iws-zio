package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Asset, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, AssetRepository}
import zio._
import zio.http._
import java.math.BigDecimal

object AssetCombinedPage:

  private val prefix = "asset"

  private def blank(company: String): Asset =
    Asset(
      id = "", name = "", description = "",
      company = company, account = "-1", oaccount = "-1",
      scrapValue = BigDecimal("1.00"), lifeSpan = 1, depMethod = 1,
      amount = BigDecimal("0.00"), rate = BigDecimal("0.00"),
      frequency = 1, currency = "EUR")

  private val pageGet: Route[AssetRepository & AccountRepository, Response] =
    Method.GET / "asset-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        (for {
          all  <- AssetRepository.all((ModelId.ASSET.modelid, company))
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
        } yield {
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          val formHtml = AssetFormView.render(blank(company), accs, mode = "view")
          val listHtml = AssetListView.render(
            page, company, "",
            rowMode  = HtmxTable.RowMode.Fetch("#asset-form"),
            detailFn = a => s"/html/asset/${a.id}/${a.modelid}/${a.company}")

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.ASSET.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">Asset</h2>
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

          HtmxResponse.html(HtmxPage.renderRaw("Asset", bodyHtml))
        }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[AssetRepository & AccountRepository, Response] = Routes(pageGet)