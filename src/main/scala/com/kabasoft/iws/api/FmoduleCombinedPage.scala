package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Fmodule, ModelId}
import com.kabasoft.iws.domain.AppError.RepositoryError
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

  private type Repos = FModuleRepository & AccountRepository

  private def buildBody(company: String): ZIO[Repos, RepositoryError, String] =
    for {
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

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.FMODULE.modelid, company)}
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
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Fmodule", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[Repos, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[Repos, Response] =
    Method.GET / "fmodule-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[Repos, Response] =
    Method.GET / "html" / "fmodule-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[Repos, Response] = Routes(pageGet, fragmentGet)