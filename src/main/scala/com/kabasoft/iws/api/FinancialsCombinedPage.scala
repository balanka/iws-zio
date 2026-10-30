package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{FinancialsTransaction, ModelId}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import java.time.{Instant, LocalDate}

object FinancialsCombinedPage:

  type Repos =
    AccountRepository & FModuleRepository & PartnerRepository &
      StoreRepository & FinancialsRepository

  private val prefix = "financials"

  private def blank(company: String, modelid: Int): FinancialsTransaction =
    val ld = LocalDate.now()
    FinancialsTransaction(
      id = -1L,
      oid = "",
      contact = "",
      costcenter = "",
      account = "-1",
      transdate = Instant.now(),
      enterdate = Instant.now(),
      postingdate = Instant.now(),
      period = ld.getYear * 100 + ld.getMonthValue,
      posted = false,
      modelid = modelid,
      company = company,
      text = "",
      footText = "",
      lines = Nil)

  /**
   * Builds the combined body (toolbar + form + list placeholder) without
   * the outer HTML page wrapper. This is the fragment that HTMX swaps into
   * #financials-combined-htmx-mount inside the React CoreUI shell.
   */
  private def buildBody(company: String): ZIO[Repos, RepositoryError, String] =
    for {
      accs   <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      stores <- StoreRepository.all((ModelId.STORE.modelid, company))
      cts    <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fms    <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
    } yield {
      val data           = FormData(accs, Nil, Nil, stores, cts, fms)
      val initialModelid =
        data.fmodules.headOption.map(_.id).getOrElse(ModelId.FINANCIALS.modelid)

      val formHtml = FinancialsFormView.render(
        t                  = blank(company, initialModelid),
        fmodules           = data.fmodules,
        stores             = data.stores,
        accounts           = data.accounts,
        contacts           = data.contacts,
        copyFromCandidates = Nil,
        mode               = "view")

      val listHtml =
        """<div id="financials-list-placeholder"
          |     class="p-4 text-center text-gray-500">
          |  Select a financials type above to see the list.
          |</div>""".stripMargin

      val toolbarHtml = HtmxToolbar.render(
        prefix,
        ModelId.FINANCIALS.modelid,
        company,
        newFormUrl     = "/html/financials/new-form",
        newFormInclude = s"#financials-form [name='modelid'], #financials-form [name='company']",
        showEdit       = false
      )

      HtmxCombinedLayout.render(
        prefix      = prefix,
        title       = s"$company/Financials",
        toolbarHtml = toolbarHtml,
        formHtml    = formHtml,
        listHtml    = listHtml
      )
    }

  /**
   * Full-page renderer — for direct browser navigation to
   * /financials-combined-htmx/:company. Wraps the body in
   * <html><head>…</head><body>…</body></html>.
   */
  private def render(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml =>
        HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Financials", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  /**
   * Fragment renderer — for HTMX swap into the React CoreUI shell.
   * Returns only the wrapper div; no <html>, <head>, or <body>.
   */
  private def renderFragment(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  // ── Routes ────────────────────────────────────────────────────────────
  //
  // /financials-combined-htmx/:company  → full page (direct browser access)
  // /html/financials-combined/:company  → fragment (HTMX fetch from React)
  //
  private val pageGet: Route[Repos, Response] =
    Method.GET / "financials-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[Repos, Response] =
    Method.GET / "html" / "financials-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet, fragmentGet)