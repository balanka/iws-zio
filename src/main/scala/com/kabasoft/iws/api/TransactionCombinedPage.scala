package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Transaction}
import com.kabasoft.iws.repository._
import zio._
import zio.http._
import java.time.{Instant, LocalDate}

object TransactionCombinedPage:

  type Repos =
    AccountRepository & ArticleRepository & FModuleRepository &
      PartnerRepository & StoreRepository & TransactionRepository & VatRepository

  private val prefix = "transaction"

  private def blank(company: String, modelid: Int): Transaction =
    val ld = LocalDate.now()
    Transaction(
      id = 0L, oid = "", contact = "", store = "", account = "-1",
      transdate = Instant.now(), enterdate = Instant.now(), postingdate = Instant.now(),
      period = ld.getYear * 100 + ld.getMonthValue,
      posted = false, modelid = modelid,
      company = company, text = "", footText = "", lines = Nil)

  private def render(company: String): ZIO[Repos, Nothing, Response] =
    (for {
      accs  <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      arts  <- ArticleRepository.all((ModelId.ARTICLE.modelid, company))
      vats  <- VatRepository.all((ModelId.VAT.modelid, company))
      stores<- StoreRepository.all((ModelId.STORE.modelid, company))
      cts   <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fms   <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
    } yield {
      val initialModelid = fms.headOption.map(_.id).getOrElse(ModelId.TRANSACTION.modelid)

      val formHtml = TransactionFormView.render(
        t        = blank(company, initialModelid),
        fmodules = fms,
        stores   = stores,
        accounts = accs,
        articles = arts,
        vats     = vats,
        contacts = cts,
        mode     = "view")

      val listHtml =
        """<div id="transaction-list-placeholder"
          |     class="p-4 text-center text-gray-500">
          |  Select a transaction type above to see the list.
          |</div>""".stripMargin

      val bodyHtml =
        s"""
           |<div class="container mx-auto max-w-5xl">
           |  <div class="htmx-page-wrapper">
           |
           |    ${HtmxToolbar.render(
                prefix,
                ModelId.TRANSACTION.modelid, company,
                newFormUrl = "/html/transaction/new-form",
                newFormInclude = s"#transaction-form [name='modelid'], #transaction-form [name='company']",
                showEdit = false
           )}
           |
           |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
           |      <div class="card-body compact">
           |        <div class="card-header-row">
           |          <h2 class="card-title"> ${company}/Transaction</h2>
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

      HtmxResponse.html(HtmxPage.renderRaw("Transaction", bodyHtml))
      HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Financials", bodyHtml))
    }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "transaction-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet)