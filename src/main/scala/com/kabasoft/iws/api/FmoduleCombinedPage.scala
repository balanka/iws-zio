package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Fmodule, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, FModuleRepository}
import zio._
import zio.http._

object FmoduleCombinedPage:

  private val prefix = "fmodule"

  private def blank(company: String): Fmodule =
    Fmodule(
      id = -1, name = "", description = "", parent = "", copyFrom = "",
      account = "-1", isDebit = false,
      accFilter = "", oaccFilter = "",
      template1 = "", template2 = "",
      modelid = ModelId.FMODULE.modelid,
      company = company)

  private val pageGet: Route[FModuleRepository & AccountRepository, Response] =
    Method.GET / "fmodule-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        (for {
          all  <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
        } yield {
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          val formHtml = FmoduleFormView.render(blank(company), accs, mode = "view")
          val listHtml = FmoduleListView.render(
            page, company, "",
            rowMode  = HtmxTable.RowMode.Fetch("#fmodule-form"),
            detailFn = m => s"/html/fmodule/${m.id}/${m.modelid}/${m.company}")

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.FMODULE.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">Fmodule</h2>
               |          <button type="button" class="btn btn-ghost btn-xs"
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

          HtmxResponse.html(HtmxPage.renderRaw("Fmodule", bodyHtml))
        }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[FModuleRepository & AccountRepository, Response] = Routes(pageGet)