package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{FinancialsTransaction, ModelId}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import java.time.{Instant, LocalDate}

object FinancialsCombinedPage:

  type Repos =
    AccountRepository & FModuleRepository & FinancialsTransactionRepository &
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
      accs  <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      stores<- StoreRepository.all((ModelId.STORE.modelid, company))
      cts   <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fms   <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
    } yield {
      val initialModelid = fms.headOption.map(_.id).getOrElse(ModelId.FINANCIALS.modelid)

      val formHtml = FinancialsFormView.render(
        t        = blank(company, initialModelid),
        fmodules = fms,
        stores   = stores,
        accounts = accs,
        contacts = cts,
        mode     = "view")

      val listHtml =
        """<div id="financials-list-placeholder"
          |     class="p-4 text-center text-gray-500">
          |  Select a financials type above to see the list.
          |</div>""".stripMargin

      val bodyHtml =
        s"""
           |<div class="container mx-auto max-w-5xl">
           |  <div class="htmx-page-wrapper">
           |
           |    ${HtmxToolbar.render(
          prefix,
          ModelId.FINANCIALS.modelid,
          company,
          newFormUrl     = "/html/financials/new-form",
          newFormInclude = s"#financials-form [name='modelid'], #financials-form [name='company']"
        )}
           |
           |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
           |      <div class="card-body compact">
           |        <div class="card-header-row">
           |          <h2 class="card-title">${company}/Financials</h2>
           |          <button type="button" class="btn btn-ghost btn-xs"
           |                  data-toggle="#$prefix-form-card"
           |                  data-label-show="Show" data-label-hide="Hide">
           |            <span class="toggle-arrow">▾</span>
           |            <span class="toggle-label">Hide</span>
           |          </button>
           |        </div>
           |        <div class="card-content">$formHtml</div>
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

      HtmxResponse.html(HtmxPage.renderRaw("Financials", bodyHtml))
    }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "financials-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet)