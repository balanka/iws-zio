package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.repository.MasterfileRepository
import zio._
import zio.http._

object MasterfileHtmlEndpoint:

  private def blank(modelid: Int, company: String): Masterfile =
    Masterfile(id = "", modelid = modelid, company = company)

  private def applyParams(existing: Masterfile, params: Map[String, String]): Masterfile =
    existing.copy(
      id          = params.getOrElse("id_display", existing.id),
      name        = params.getOrElse("name", existing.name),
      description = params.getOrElse("description", existing.description),
      parent      = params.getOrElse("parent", existing.parent))

  private val listGet: Route[MasterfileRepository, Response] =
    Method.GET / "html" / "masterfile-list" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)

        MasterfileRepository.all((modelid, company)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(m => m.id.contains(query) ||
              m.name.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "name"        => x.name.trim.compareTo(y.name.trim)
                case "description" => x.description.trim.compareTo(y.description.trim)
                case "parent"      => x.parent.trim.compareTo(y.parent.trim)
                case _             => x.id.trim.compareTo(y.id.trim)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            MasterfileListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              modelid, company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[Any, Response] =
    Method.GET / "html" / "masterfile" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        ZIO.succeed(HtmxResponse.html(
          MasterfileFormView.render(blank(modelid, company), mode = "create")))
      }

  private val formGet: Route[MasterfileRepository, Response] =
    Method.GET / "html" / "masterfile" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        MasterfileRepository.getById((id, modelid, company))
          .map(m => HtmxResponse.html(MasterfileFormView.render(m)))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[MasterfileRepository, Response] =
    Method.PUT / "html" / "masterfile" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.getOrElse("id", "")
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(-1)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(modelid, company))
          else MasterfileRepository.getById((id, modelid, company))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then MasterfileRepository.create(toSave).unit
          else MasterfileRepository.modify(toSave).unit
          fresh    <- MasterfileRepository.getById((toSave.id, modelid, company))
        } yield HtmxResponse.html(MasterfileFormView.render(fresh)))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[MasterfileRepository, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug