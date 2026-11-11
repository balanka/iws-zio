package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Contact, ModelId}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.PartnerRepository
import zio._
import zio.http._

object ContactCombinedPage:

  private val prefix = "contact"

  private def blank(company: String): Contact =
    Contact(
      id = "", name = "", description = "", street = "", zip = "",
      city = "", state = "", country = "", phone = "", email = "",
      company = company)

  private def buildBody(company: String): ZIO[PartnerRepository, RepositoryError, String] =
    PartnerRepository.all((ModelId.CONTACT.modelid, company)).map { all =>
      val sorted      = all.sortBy(_.id)
      val initialSize = 20
      val slice       = sorted.take(initialSize)
      val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

      val formHtml = ContactFormView.render(blank(company), mode = "view")
      val listHtml = ContactListView.render(
        page, company, "",
        rowMode  = HtmxTable.RowMode.Fetch("#contact-form"),
        detailFn = c => s"/html/contact/${c.id}/${c.modelid}/${c.company}")

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.CONTACT.modelid, company)}
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

  private def render(company: String): ZIO[PartnerRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Contact", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[PartnerRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[PartnerRepository, Response] =
    Method.GET / "contact-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[PartnerRepository, Response] =
    Method.GET / "html" / "contact-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[PartnerRepository, Response] = Routes(pageGet, fragmentGet)