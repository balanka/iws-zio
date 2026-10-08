package com.kabasoft.iws.api

import com.kabasoft.iws.domain.ModelId
import com.kabasoft.iws.repository.{AccountRepository, PartnerRepository, VatRepository}
import zio._
import zio.http._

object BusinessPartnerCombinedPage:

  type Repos = PartnerKind.Env & AccountRepository & VatRepository & PartnerRepository

  private def render(kind: PartnerKind, company: String)
  : ZIO[Repos, Nothing, Response] =
    val prefix = kind.urlPrefix
    (for {
      all      <- PartnerKind.loadAll(kind, company)
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      vats     <- VatRepository.all((ModelId.VAT.modelid, company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
    } yield {
      val sorted      = all.sortBy(_.id)
      val initialSize = 20
      val slice       = sorted.take(initialSize)
      val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

      // Default form is read-only view mode.
      val formHtml = BusinessPartnerFormView.render(
        PartnerKind.blank(kind, company), accs, vats, contacts, kind, mode = "view")
      val listHtml = BusinessPartnerListView.render(page, company, "", kind)

      val bodyHtml =
        s"""
           |<div class="container mx-auto max-w-5xl">
           |  <div class="htmx-page-wrapper">
           |
           |    ${HtmxToolbar.render(prefix, kind.modelid, company)}
           |
           |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
           |      <div class="card-body compact">
           |        <div class="card-header-row">
           |          <h2 class="card-title">${kind.title}</h2>
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

      HtmxResponse.html(HtmxPage.renderRaw(kind.title, bodyHtml))
    }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val customerPage: Route[Repos, Response] =
    Method.GET / "customer-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(PartnerKind.Customer, company) }

  private val supplierPage: Route[Repos, Response] =
    Method.GET / "supplier-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(PartnerKind.Supplier, company) }

  private val employeePage: Route[Repos, Response] =
    Method.GET / "employee-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(PartnerKind.Employee, company) }

  val routes: Routes[Repos, Response] =
    Routes(customerPage, supplierPage, employeePage)