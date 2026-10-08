package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Permission}
import com.kabasoft.iws.repository.PermissionRepository
import zio._
import zio.http._

object PermissionCombinedPage:

  private val prefix = "permission"

  private def blank(company: String): Permission =
    Permission(id = -1, name = "", description = "", short = "", modelid = ModelId.PERMISSION.modelid, company = company)

  private val pageGet: Route[PermissionRepository, Response] =
    Method.GET / "permission-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
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

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.PERMISSION.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">Permission</h2>
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

          HtmxResponse.html(HtmxPage.renderRaw("Permission", bodyHtml))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[PermissionRepository, Response] = Routes(pageGet)