package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.repository.MasterfileRepository
import zio._
import zio.http._

object MasterfileCombinedPage:

  private def buildBody(
                         modelid: Int,
                         company: String,
                         title:   String
                       ): ZIO[MasterfileRepository, RepositoryError, String] =
    MasterfileRepository.all((modelid, company)).map { all =>
      val sorted      = all.sortBy(_.id)
      val initialSize = 20
      val slice       = sorted.take(initialSize)
      val page        = HtmxTable.Page(slice, 0, initialSize, sorted.size.toLong)

      val formHtml = MasterfileFormView.render(
        Masterfile(id = "", modelid = modelid, company = company))
      val listHtml = MasterfileListView.render(page, modelid, company, "")

      s"""
         |<div class="container mx-auto max-w-5xl">
         |  <div class="htmx-page-wrapper">
         |
         |    ${HtmxToolbar.render("masterfile", modelid, company)}
         |
         |    <div id="masterfile-form-card" class="daisy-card bg-base-100 shadow-sm">
         |      <div class="daisy-card-body compact">
         |        <div class="daisy-card-content">
         |          <div id="masterfile-form">$formHtml</div>
         |        </div>
         |      </div>
         |    </div>
         |
         |    <div id="masterfile-list-card" class="daisy-card bg-base-100 shadow-sm">
         |      <div class="daisy-card-body compact">
         |        <div class="daisy-card-content">$listHtml</div>
         |      </div>
         |    </div>
         |
         |  </div>
         |</div>
         |""".stripMargin
    }

  private def render(modelid: Int, company: String, title: String)
  : ZIO[MasterfileRepository, Nothing, Response] =
    buildBody(modelid, company, title)
      .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw(title, bodyHtml)))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private def renderFragment(modelid: Int, company: String, title: String)
  : ZIO[MasterfileRepository, Nothing, Response] =
    buildBody(modelid, company, title)
      .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  private val pageGet: Route[MasterfileRepository, Response] =
    Method.GET / "masterfile-combined-htmx" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, req: Request) =>
        val lang  = Messages.langFrom(req.rawHeader("accept-language"))
        val title = Messages.masterfileTitle(modelid, lang)
        render(modelid, company, title)
      }

  private val fragmentGet: Route[MasterfileRepository, Response] =
    Method.GET / "html" / "masterfile-combined" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, req: Request) =>
        val lang  = Messages.langFrom(req.rawHeader("accept-language"))
        val title = Messages.masterfileTitle(modelid, lang)
        renderFragment(modelid, company, title)
      }

  val routes: Routes[MasterfileRepository, Response] = Routes(pageGet, fragmentGet)