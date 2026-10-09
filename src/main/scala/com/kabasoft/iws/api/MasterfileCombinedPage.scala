package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.repository.MasterfileRepository
import zio._
import zio.http._

object MasterfileCombinedPage:

  private val pageGet: Route[MasterfileRepository, Response] =
    Method.GET / "masterfile-combined-htmx" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, req: Request) =>
        val lang  = Messages.langFrom(req.rawHeader("accept-language"))
        val title = Messages.masterfileTitle(modelid, lang)
        MasterfileRepository.all((modelid, company)).map { all =>
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          val formHtml = MasterfileFormView.render(
            Masterfile(id = "", modelid = modelid, company = company))
          val listHtml = MasterfileListView.render(page, modelid, company, "")

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render("masterfile", modelid, company)}
               |
               |    <div id="masterfile-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">${Html.esc(title)}</h2>
               |          <button type="button" class="btn btn-ghost btn-xs"
               |                  data-toggle="#masterfile-form-card"
               |                  data-label-show="Show" data-label-hide="Hide">
               |            <span class="toggle-arrow">▾</span>
               |            <span class="toggle-label">Hide</span>
               |          </button>
               |        </div>
               |        <div class="card-content">
               |          <div id="masterfile-form">$formHtml</div>
               |        </div>
               |      </div>
               |    </div>
               |
               |    <div id="masterfile-list-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-content">$listHtml</div>
               |      </div>
               |    </div>
               |
               |  </div>
               |</div>
               |""".stripMargin

          HtmxResponse.html(HtmxPage.renderRaw(title, bodyHtml))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[MasterfileRepository, Response] = Routes(pageGet)