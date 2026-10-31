package com.kabasoft.iws.api

import com.kabasoft.iws.domain.ModelId
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.{
  AccountRepository,
  MasterfileRepository,
  PartnerRepository,
  VatRepository
}
import zio._
import zio.http._

object BusinessPartnerCombinedPage:

  type Repos = PartnerKind.Env & AccountRepository & VatRepository &
    PartnerRepository & MasterfileRepository

  private def buildBody(kind: PartnerKind, company: String): ZIO[Repos, RepositoryError, String] =
    val prefix = kind.urlPrefix
    for {
      all      <- PartnerKind.loadAll(kind, company)
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      vats     <- VatRepository.all((ModelId.VAT.modelid, company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      banks    <- MasterfileRepository.all((ModelId.BANK.modelid, company))
    } yield {
      val sorted      = all.sortBy(_.id)
      val initialSize = 20
      val slice       = sorted.take(initialSize)
      val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

      val formHtml = BusinessPartnerFormView.render(
        PartnerKind.blank(kind, company), accs, vats, contacts, banks, kind, mode = "view")
      val listHtml = BusinessPartnerListView.render(page, company, "", kind)

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, kind.modelid, company)}
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

  private def render(kind: PartnerKind, company: String): ZIO[Repos, Nothing, Response] =
    buildBody(kind, company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw(kind.title, bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(kind: PartnerKind, company: String): ZIO[Repos, Nothing, Response] =
    buildBody(kind, company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val customerPage: Route[Repos, Response] =
    Method.GET / "customer-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(PartnerKind.Customer, company) }

  private val supplierPage: Route[Repos, Response] =
    Method.GET / "supplier-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(PartnerKind.Supplier, company) }

  private val employeePage: Route[Repos, Response] =
    Method.GET / "employee-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(PartnerKind.Employee, company) }

  private val customerFragment: Route[Repos, Response] =
    Method.GET / "html" / "customer-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(PartnerKind.Customer, company) }

  private val supplierFragment: Route[Repos, Response] =
    Method.GET / "html" / "supplier-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(PartnerKind.Supplier, company) }

  private val employeeFragment: Route[Repos, Response] =
    Method.GET / "html" / "employee-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(PartnerKind.Employee, company) }

  val routes: Routes[Repos, Response] =
    Routes(customerPage, supplierPage, employeePage,
      customerFragment, supplierFragment, employeeFragment)