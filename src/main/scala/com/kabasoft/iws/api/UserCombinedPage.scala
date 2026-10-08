package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, User}
import com.kabasoft.iws.repository.UserRepository
import zio._
import zio.http._

object UserCombinedPage:

  private val prefix = "user"

  private def blank(company: String): User =
    User(
      id = -1, userName = "", firstName = "", lastName = "",
      hash = "", phone = "", email = "", department = "", menu = "",
      company = company, modelid = ModelId.USER.modelid,
      roles = Nil, rights = Nil, modules = Nil)

  private val pageGet: Route[UserRepository, Response] =
    Method.GET / "user-combined-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        UserRepository.all((ModelId.USER.modelid, company)).map { all =>
          val sorted      = all.sortBy(_.id)
          val initialSize = 20
          val slice       = sorted.take(initialSize)
          val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

          val formHtml = UserFormView.render(blank(company), mode = "view")
          val listHtml = UserListView.render(
            page, company, "",
            rowMode  = HtmxTable.RowMode.Fetch("#user-form"),
            detailFn = u => s"/html/user/${u.id}/${u.modelid}/${u.company}")

          val bodyHtml =
            s"""
               |<div class="container mx-auto max-w-5xl">
               |  <div class="htmx-page-wrapper">
               |
               |    ${HtmxToolbar.render(prefix, ModelId.USER.modelid, company)}
               |
               |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
               |      <div class="card-body compact">
               |        <div class="card-header-row">
               |          <h2 class="card-title">User</h2>
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

          HtmxResponse.html(HtmxPage.renderRaw("User", bodyHtml))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[UserRepository, Response] = Routes(pageGet)