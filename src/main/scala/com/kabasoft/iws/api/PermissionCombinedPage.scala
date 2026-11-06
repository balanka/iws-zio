package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Permission}
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.PermissionRepository
import zio._
import zio.http._

object PermissionCombinedPage:

  private val prefix = "permission"

  private def blank(company: String): Permission =
    Permission(id = -1, name = "", description = "", short = "",
      modelid = ModelId.PERMISSION.modelid, company = company)

  private def buildBody(company: String): ZIO[PermissionRepository, RepositoryError, String] =
    PermissionRepository.all((ModelId.PERMISSION.modelid, company)).map { all =>
      val sorted      = all.sortBy(_.id)
      val initialSize = 20
      val slice       = sorted.take(initialSize)
      val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

      val formHtml = PermissionFormView.render(blank(company), mode = "view")
      val listHtml = PermissionListView.render(
        page, company, "",
        rowMode  = HtmxTable.RowMode.Fetch("#permission-form"),
        detailFn = p => s"/html/permission/${p.id}/${p.modelid}/${p.company}")

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render(prefix, ModelId.PERMISSION.modelid, company)}
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

  private def render(company: String): ZIO[PermissionRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw("Permission", bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(company: String): ZIO[PermissionRepository, Nothing, Response] =
    buildBody(company)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[PermissionRepository, Response] =
    Method.GET / "permission-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) => render(company) }

  private val fragmentGet: Route[PermissionRepository, Response] =
    Method.GET / "html" / "permission-combined" / string("company") ->
      handler { (company: String, _: Request) => renderFragment(company) }

  val routes: Routes[PermissionRepository, Response] = Routes(pageGet, fragmentGet)