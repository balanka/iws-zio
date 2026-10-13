package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{FinancialsTransaction, ModelId}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import java.time.{Instant, LocalDate}

object FinancialsCombinedPage:

  type Repos =
    AccountRepository & FModuleRepository & FinancialsRepository &
      PartnerRepository & StoreRepository

  private val prefix = "financials"

  private def blank(company: String, modelid: Int): FinancialsTransaction =
    val ld = LocalDate.now()
    FinancialsTransaction(
      id = 0L, oid = "", contact = "", costcenter = "", account = "-1",
      transdate = Instant.now(), enterdate = Instant.now(), postingdate = Instant.now(),
      period = ld.getYear * 100 + ld.getMonthValue,
      posted = false, modelid = modelid,
      company = company, text = "", footText = "", lines = Nil)

  private def render(company: String): ZIO[Repos, Nothing, Response] =
    (for {
      accs   <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      stores <- StoreRepository.all((ModelId.STORE.modelid, company))
      cts    <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fms    <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
    } yield {
      val data           = FormData(accounts = accs, stores = stores, contacts = cts, fmodules = fms)
      val initialModelid = data.fmodules.headOption.map(_.id).getOrElse(ModelId.FINANCIALS.modelid)

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

      val bodyHtml = HtmxCombinedLayout.render(
        prefix      = prefix,
        title       = s"$company/Financials",
        toolbarHtml = toolbarHtml,
        formHtml    = formHtml,
        listHtml    = listHtml
      )

      HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Financials", bodyHtml))
    }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "financials-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet)