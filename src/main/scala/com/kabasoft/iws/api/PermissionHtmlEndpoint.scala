package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Permission}
import com.kabasoft.iws.repository.PermissionRepository
import zio._
import zio.http._

object PermissionHtmlEndpoint:

  type Repos = PermissionRepository

  private def blank(company: String): Permission =
    Permission(
      id = -1, name = "", description = "", short = "",
      modelid = ModelId.PERMISSION.modelid,
      company = company)

  private def renderPermission(p: Permission, mode: String = "view")
  : ZIO[Any, Nothing, String] = ZIO.succeed(PermissionFormView.render(p, mode))

  private def applyParams(existing: Permission, params: Map[String, String]): Permission =
    existing.copy(
      id          = params.get("id_display").flatMap(_.toIntOption).getOrElse(existing.id),
      name        = params.getOrElse("name", existing.name),
      description = params.getOrElse("description", existing.description),
      short       = params.getOrElse("short", existing.short))

  private val listGet: Route[PermissionRepository, Response] =
    Method.GET / "html" / "permission-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)
        PermissionRepository.all((ModelId.PERMISSION.modelid, company)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(p => p.id.toString.contains(query) ||
              p.name.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "name"        => x.name.trim.compareTo(y.name.trim)
                case "short"       => x.short.trim.compareTo(y.short.trim)
                case "description" => x.description.trim.compareTo(y.description.trim)
                case _             => x.id.compareTo(y.id)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            PermissionListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[Any, Response] =
    Method.GET / "html" / "permission" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        ZIO.succeed(HtmxResponse.html(
          PermissionFormView.render(blank(company), mode = "create")))
      }

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "permission" / int("id") / int("modelid") / string("company") ->
      handler { (id: Int, modelid: Int, company: String, _: Request) =>
        (for {
          p    <- PermissionRepository.getById((id, modelid, company))
          html <- renderPermission(p)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "permission" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.get("id").flatMap(_.toIntOption).getOrElse(-1)
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(ModelId.PERMISSION.modelid)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else PermissionRepository.getById((id, modelid, company))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then PermissionRepository.create(toSave).unit
          else PermissionRepository.modify(toSave).unit
          fresh    <- PermissionRepository.getById((toSave.id, modelid, company))
          html     <- renderPermission(fresh)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug