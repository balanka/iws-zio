package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Store}
import com.kabasoft.iws.repository.{AccountRepository, MasterfileRepository, StoreRepository}
import zio._
import zio.http._

object StoreCombinedPage:

  private val prefix = "store"

  private def blank(company: String): Store =
    Store(
      id = "", name = "", description = "", costcenter = "",
      account = "-1", oaccount = "-1", company = company, stocks = Nil)

  private val pageGet: Route[StoreRepository & AccountRepository & MasterfileRepository, Response] =
    Method.GET / "store-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        (for {
          all  <- StoreRepository.all((ModelId.STORE.modelid, company))
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          ccs  <- MasterfileRepository.all((ModelId.COST_CEMTER.modelid, company))
        } yield {
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          val formHtml = StoreFormView.render(blank(company), accs, ccs, mode = "view")
          val listHtml = StoreListView.render(
            page, company, "",
            rowMode  = HtmxTable.RowMode.Fetch("#store-form"),
            detailFn = s => s"/html/store/${s.id}/${s.modelid}/${s.company}")

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.STORE.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">Store</h2>
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

          HtmxResponse.html(HtmxPage.renderRaw("Store", bodyHtml))
        }).catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[StoreRepository & AccountRepository & MasterfileRepository, Response] =
    Routes(pageGet)