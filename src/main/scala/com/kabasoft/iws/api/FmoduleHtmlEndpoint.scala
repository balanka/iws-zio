package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Fmodule, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, FModuleRepository}
import zio._
import zio.http._

object FmoduleHtmlEndpoint:

  type Repos = FModuleRepository & AccountRepository

  private def blank(company: String): Fmodule =
    Fmodule(id = -1, name = "", description = "", parent = "", copyFrom = "",
      account = "-1", isDebit = false, accFilter = "", oaccFilter = "", template1 = "", template2 = "",
      modelid = ModelId.FMODULE.modelid, company = company)

  private def renderFmodule(m: Fmodule, mode: String = "view")
  : ZIO[AccountRepository, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    AccountRepository.all((ModelId.ACCOUNT.modelid, m.company))
      .map(accounts => FmoduleFormView.render(m, accounts, mode))

  private def applyParams(existing: Fmodule, params: Map[String, String]): Fmodule =
    existing.copy(
      id          = params.get("id_display").flatMap(_.toIntOption).getOrElse(existing.id),
      name        = params.getOrElse("name", existing.name),
      description = params.getOrElse("description", existing.description),
      parent      = params.getOrElse("parent", existing.parent),
      copyFrom    = params.getOrElse("copyFrom", existing.copyFrom),
      account     = params.getOrElse("account", existing.account),
      accFilter   = params.getOrElse("accFilter", existing.accFilter),
      oaccFilter  = params.getOrElse("oaccFilter", existing.oaccFilter),
      template1   = params.getOrElse("template1", existing.template1),
      template2   = params.getOrElse("template2", existing.template2),
      isDebit     = params.contains("isDebit"))

  private val listGet: Route[FModuleRepository, Response] =
    Method.GET / "html" / "fmodule-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)
        FModuleRepository.all((ModelId.FMODULE.modelid, company)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(m => m.id.toString.contains(query) ||
              m.name.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "name"        => x.name.trim.compareTo(y.name.trim)
                case "parent"      => x.parent.trim.compareTo(y.parent.trim)
                case "description" => x.description.trim.compareTo(y.description.trim)
                case _             => x.id.compareTo(y.id)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            FmoduleListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[AccountRepository, Response] =
    Method.GET / "html" / "fmodule" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          .map(accs => HtmxResponse.html(
            FmoduleFormView.render(blank(company), accs, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "fmodule" / int("id") / int("modelid") / string("company") ->
      handler { (id: Int, modelid: Int, company: String, _: Request) =>
        (for {
          m    <- FModuleRepository.getById((id, modelid, company))
          html <- renderFmodule(m)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "fmodule" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.get("id").flatMap(_.toIntOption).getOrElse(-1)
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(ModelId.FMODULE.modelid)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else FModuleRepository.getById((id, modelid, company))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then FModuleRepository.create(toSave).unit
          else FModuleRepository.modify(toSave).unit
          fresh    <- FModuleRepository.getById((toSave.id, modelid, company))
          html     <- renderFmodule(fresh)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug