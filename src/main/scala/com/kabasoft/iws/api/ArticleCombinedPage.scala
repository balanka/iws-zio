package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Article, ModelId}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, VatRepository}
import zio._
import zio.http._
import java.math.BigDecimal

object ArticleCombinedPage:

  private val prefix = "article"

  private type Repos = ArticleRepository & AccountRepository & VatRepository

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

  private def buildBody(company: String): ZIO[Repos, RepositoryError, String] =
    for {
      all  <- ArticleRepository.all((ModelId.ARTICLE.modelid, company))
      accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      vats <- VatRepository.all((ModelId.VAT.modelid, company))
    } yield {
      val sorted      = all.sortBy(_.id)
      val initialSize = 20
      val slice       = sorted.take(initialSize)
      val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

      val formHtml = ArticleFormView.render(blank(company), accs, vats, mode = "view")
      val listHtml = ArticleListView.render(
        page, company, "",
        rowMode  = HtmxTable.RowMode.Fetch("#article-form"),
        detailFn = a => s"/html/article/${a.id}/${a.modelid}/${a.company}"
      )

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.ARTICLE.modelid, company)}
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
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Article", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "article-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[Repos, Response] =
    Method.GET / "html" / "article-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet, fragmentGet)