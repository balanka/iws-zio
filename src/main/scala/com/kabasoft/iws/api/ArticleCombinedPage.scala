package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Article, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, VatRepository}
import zio._
import zio.http._
import java.math.BigDecimal

object ArticleCombinedPage:

  private val prefix = "article"

  private def blank(company: String): Article =
    Article(
      id             = "",
      name           = "",
      description    = "",
      parent         = "",
      sprice         = BigDecimal("0.00"),
      pprice         = BigDecimal("0.00"),
      avgPrice       = BigDecimal("0.00"),
      currency       = "EUR",
      stocked        = false,
      quantityUnit   = "",
      packUnit       = "",
      account        = "-1",
      oaccount       = "-1",
      revenueAccount = "-1",
      vatCode        = "-1",
      company        = company
    )

  private val pageGet: Route[ArticleRepository & AccountRepository & VatRepository, Response] =
    Method.GET / "article-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        (for {
          all  <- ArticleRepository.all((ModelId.ARTICLE.modelid, company))
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          vats <- VatRepository.all((ModelId.VAT.modelid, company))
        } yield {
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          // Default form is read-only view mode.
          val formHtml = ArticleFormView.render(blank(company), accs, vats, mode = "view")
          val listHtml = ArticleListView.render(
            page, company, "",
            rowMode  = HtmxTable.RowMode.Fetch("#article-form"),
            detailFn = a => s"/html/article/${a.id}/${a.modelid}/${a.company}"
          )

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.ARTICLE.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">Article</h2>
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

          HtmxResponse.html(HtmxPage.renderRaw("Article", bodyHtml))
        }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[ArticleRepository & AccountRepository & VatRepository, Response] =
    Routes(pageGet)