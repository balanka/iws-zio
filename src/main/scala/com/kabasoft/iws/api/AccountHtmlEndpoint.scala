package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, ModelId}
import com.kabasoft.iws.repository.AccountRepository
import zio.*
import zio.http.*

object AccountHtmlEndpoint:

  private def html(body: String): Response =
    Response(
      status = Status.Ok,
      headers = Headers(Header.ContentType(MediaType.text.html)),
      body = Body.fromString(body, java.nio.charset.StandardCharsets.UTF_8)
    )

  private def errorHtml(message: String): Response =
    Response(
      status = Status.InternalServerError,
      headers = Headers(Header.ContentType(MediaType.text.html)),
      body = Body.fromString(s"""<p style="color:red">Error: $message</p>""", java.nio.charset.StandardCharsets.UTF_8)
    )

  private val listGet: Route[AccountRepository, Response] =
    Method.GET / "html" / "account-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort = req.queryParam("sort")
          .map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty)
          .getOrElse(HtmxTable.Page.defaultSort)

        AccountRepository
          .all((ModelId.ACCOUNT.modelid, company))
          .map { all =>
            val filtered =
              if query.isEmpty then all
              else all.filter(a =>
                a.id.contains(query) || a.name.toLowerCase.contains(query.toLowerCase)
              )

            // Multi-key sort: apply in reverse order so the first
            // spec entry ends up as the primary key (List.sorted is
            // stable, so the last sort applied becomes primary).
            val sorted = filtered.sortWith { (a, b) =>
              val cmp = sort.iterator
                .map { case (key, dir) =>
                  val c = key match
                    case "name"        => a.name.trim.compareTo(b.name.trim)
                    case "description" => a.description.trim.compareTo(b.description.trim)
                    case "currency"    => a.currency.trim.compareTo(b.currency.trim)
                    case _             => a.id.trim.compareTo(b.id.trim)
                  if dir == "desc" then -c else c
                }
                .find(_ != 0)
                .getOrElse(0)
              cmp < 0
            }

            val slice = sorted.slice(page * size, page * size + size)
            html(
              AccountListView.render(
                HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
                company,
                query
              )
            )
          }
          .catchAll(err => ZIO.succeed(errorHtml(err.toString)))
      }

  private val formGet: Route[AccountRepository, Response] =
    Method.GET / "html" / "account" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        HtmxEndpoint.loadForm(
          id, modelid, company,
          load = (id, modelid, company) => AccountRepository.getById((id, modelid, company)),
          render = (acc: com.kabasoft.iws.domain.Account) =>
            AccountRepository.all((ModelId.ACCOUNT.modelid, acc.company))
              .map(all => AccountFormView.render(acc, all))
        )
      }

  private val formPut: Route[AccountRepository, Response] =
    Method.PUT / "html" / "account" ->
      handler { (req: Request) =>
        HtmxEndpoint.saveForm(
          req,
          load = (id, modelid, company) => AccountRepository.getById((id, modelid, company)),
          update = (existing: com.kabasoft.iws.domain.Account, params) => existing.copy(
            name         = params.getOrElse("name", existing.name),
            description  = params.getOrElse("description", existing.description),
            account      = params.getOrElse("account", existing.account),
            currency     = params.getOrElse("currency", existing.currency),
            isDebit      = params.contains("isDebit"),
            balancesheet = params.contains("balancesheet")
          ),
          save = (acc: com.kabasoft.iws.domain.Account) => AccountRepository.modify(acc),
          render = (acc: com.kabasoft.iws.domain.Account) =>
            AccountRepository.all((ModelId.ACCOUNT.modelid, acc.company))
              .map(all => AccountFormView.render(acc, all))
        )
      }

  val routes: Routes[AccountRepository, Response] = Routes(listGet, formGet, formPut) @@ Middleware.debug
