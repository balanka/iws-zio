package com.kabasoft.iws.api

import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.http._

/**
 * Rendering + routing for "*CombinedPage" endpoints.
 * Loading is delegated entirely to FormLists (single loader).
 */
object CombinedPageSupport:

  /**
   * Build the body HTML for a combined page.
   * `renderForm(company, data, initialModelid)` renders the form.
   */
  def buildBody(
                 prefix:             String,
                 parentModelId:      Int,
                 company:            String,
                 data:               FormLists.FormData,
                 title:              String,
                 formId:             String,
                 newFormUrl:         String,
                 newFormInclude:     String,
                 listPlaceholderId:  String,
                 listPlaceholderText:String,
                 showEdit:           Boolean,
                 renderForm:         (String, FormLists.FormData, Int) => String
               ): String =
    val initialModelid =
      data.fmodules.headOption.map(_.id).getOrElse(parentModelId)

    val formHtml = renderForm(company, data, initialModelid)

    val listHtml =
      s"""<div id="$listPlaceholderId"
         |     class="p-4 text-center text-gray-500">
         |  $listPlaceholderText
         |</div>""".stripMargin

    val toolbarHtml = HtmxToolbar.render(
      prefix, parentModelId, company,
      newFormUrl = newFormUrl, newFormInclude = newFormInclude, showEdit = showEdit)

    HtmxCombinedLayout.render(
      prefix      = prefix,
      title       = s"$company/$title",
      toolbarHtml = toolbarHtml,
      formHtml    = formHtml,
      listHtml    = listHtml)

  /**
   * Base variant: load via FormLists.loadBase, routes for full page + fragment.
   */
  def routesBase(
                  fullPathSegment:     String,
                  fragmentPathSegment: String,
                  parentModelId:       Int,
                  title:               String,
                  formId:              String,
                  newFormUrl:          String,
                  newFormInclude:      String,
                  listPlaceholderId:   String,
                  listPlaceholderText: String,
                  renderForm:          (String, FormLists.FormData, Int) => String
                ): Routes[FormLists.BaseEnv & FormLists.Cache, Response] =

    def build(company: String): ZIO[FormLists.BaseEnv & FormLists.Cache, RepositoryError, String] =
      FormLists.loadBase(company, parentModelId).map { data =>
        buildBody(
          prefix             = title.toLowerCase,
          parentModelId      = parentModelId,
          company            = company,
          data               = data,
          title              = title,
          formId             = formId,
          newFormUrl         = newFormUrl,
          newFormInclude     = newFormInclude,
          listPlaceholderId  = listPlaceholderId,
          listPlaceholderText= listPlaceholderText,
          showEdit           = false,
          renderForm         = renderForm)
      }

    val pageGet: Route[FormLists.BaseEnv & FormLists.Cache, Response] =
      Method.GET / fullPathSegment / string("company") ->
        handler { (company: String, _: Request) =>
          build(company)
            .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw(title, bodyHtml)))
            .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
        }

    val fragmentGet: Route[FormLists.BaseEnv & FormLists.Cache, Response] =
      Method.GET / "html" / fragmentPathSegment / string("company") ->
        handler { (company: String, _: Request) =>
          build(company)
            .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
            .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
        }

    Routes(pageGet, fragmentGet) @@ Middleware.debug

  /**
   * With-articles-and-vats variant: load via FormLists.loadWithArticlesAndVats.
   */
  def routesWithArticlesAndVats(
                                 fullPathSegment:     String,
                                 fragmentPathSegment: String,
                                 parentModelId:       Int,
                                 title:               String,
                                 formId:              String,
                                 newFormUrl:          String,
                                 newFormInclude:      String,
                                 listPlaceholderId:   String,
                                 listPlaceholderText: String,
                                 renderForm:          (String, FormLists.FormData, Int) => String
                               ): Routes[FormLists.WithArticlesAndVats & FormLists.Cache, Response] =

    def build(company: String)
    : ZIO[FormLists.WithArticlesAndVats & FormLists.Cache, RepositoryError, String] =
      FormLists.loadWithArticlesAndVats(company, parentModelId).map { data =>
        buildBody(
          prefix             = title.toLowerCase,
          parentModelId      = parentModelId,
          company            = company,
          data               = data,
          title              = title,
          formId             = formId,
          newFormUrl         = newFormUrl,
          newFormInclude     = newFormInclude,
          listPlaceholderId  = listPlaceholderId,
          listPlaceholderText= listPlaceholderText,
          showEdit           = false,
          renderForm         = renderForm)
      }

    val pageGet: Route[FormLists.WithArticlesAndVats & FormLists.Cache, Response] =
      Method.GET / fullPathSegment / string("company") ->
        handler { (company: String, _: Request) =>
          build(company)
            .map(bodyHtml => HtmxResponse.htmlNoCache(HtmxPage.renderRaw(title, bodyHtml)))
            .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
        }

    val fragmentGet: Route[FormLists.WithArticlesAndVats & FormLists.Cache, Response] =
      Method.GET / "html" / fragmentPathSegment / string("company") ->
        handler { (company: String, _: Request) =>
          build(company)
            .map(bodyHtml => HtmxResponse.htmlNoCache(bodyHtml))
            .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
        }

    Routes(pageGet, fragmentGet) @@ Middleware.debug