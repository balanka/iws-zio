package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Asset, ModelId}
import com.kabasoft.iws.domain.AppError.RepositoryError
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

  private type Repos = AssetRepository & AccountRepository

  private def buildBody(company: String): ZIO[Repos, RepositoryError, String] =
    for {
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

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.ASSET.modelid, company)}
         |
         |    <div id="$prefix-form-card" class="daisy-card bg-base-100 shadow-sm">
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
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Asset", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "asset-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[Repos, Response] =
    Method.GET / "html" / "asset-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet, fragmentGet)