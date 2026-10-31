package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Vat}
import com.kabasoft.iws.domain.AppError.RepositoryError
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

  /**
   * Builds the combined body (toolbar + form + list placeholder) without
   * the outer HTML page wrapper. This is the fragment that HTMX swaps into
   * #vat-combined-htmx-mount inside the React CoreUI shell.
   */
  private def buildBody(
                         company: String
                       ): ZIO[VatRepository & AccountRepository, RepositoryError, String] =
    for {
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

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.VAT.modelid, company)}
         |
         |    <div id="$prefix-form-card" class="daisy-card bg-base-100 shadow-sm">
         |      <div class="daisy-card-body compact">
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

  /**
   * Full-page renderer — for direct browser navigation to
   * /vat-combined-htmx/:company. Wraps the body in
   * <html><head>…</head><body>…</body></html>.
   */
  private def render(
                      company: String
                    ): ZIO[VatRepository & AccountRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("VAT", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  /**
   * Fragment renderer — for HTMX swap into the React CoreUI shell.
   * Returns only the wrapper div; no <html>, <head>, or <body>.
   */
  private def renderFragment(
                              company: String
                            ): ZIO[VatRepository & AccountRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  // ── Routes ────────────────────────────────────────────────────────────

  private val pageGet: Route[VatRepository & AccountRepository, Response] =
    Method.GET / "vat-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[VatRepository & AccountRepository, Response] =
    Method.GET / "html" / "vat-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[VatRepository & AccountRepository, Response] = Routes(pageGet, fragmentGet)