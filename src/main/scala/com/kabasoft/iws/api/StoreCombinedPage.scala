package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Store}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.{AccountRepository, MasterfileRepository, StoreRepository}
import zio._
import zio.http._

object StoreCombinedPage:

  private val prefix = "store"

  private def blank(company: String): Store =
    Store(
      id = "", name = "", description = "", costcenter = "",
      account = "-1", oaccount = "-1", company = company, stocks = Nil)

  private type Repos = StoreRepository & AccountRepository & MasterfileRepository

  private def buildBody(company: String): ZIO[Repos, RepositoryError, String] =
    for {
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

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.STORE.modelid, company)}
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

  private def render(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Store", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "store-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[Repos, Response] =
    Method.GET / "html" / "store-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet, fragmentGet)