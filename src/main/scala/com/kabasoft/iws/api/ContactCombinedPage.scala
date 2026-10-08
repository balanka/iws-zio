package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Contact, ModelId}
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

  private val pageGet: Route[PartnerRepository, Response] =
    Method.GET / "contact-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
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

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.CONTACT.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">Contact</h2>
               |          <button type="button"
               |                  class="btn btn-ghost btn-xs"
               |                  data-toggle="#$prefix-form-card"
               |                  data-label-show="Show" data-label-hide="Hide">
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

          HtmxResponse.html(HtmxPage.renderRaw("Contact", bodyHtml))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[PartnerRepository, Response] = Routes(pageGet)