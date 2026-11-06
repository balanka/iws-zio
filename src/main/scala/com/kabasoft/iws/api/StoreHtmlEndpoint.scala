package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Store}
import com.kabasoft.iws.repository.{
  AccountRepository,
  MasterfileRepository,
  StockRepository,
  StoreRepository
}
import zio._
import zio.http._

object StoreHtmlEndpoint:

  // The masterfile lists and the stock list the form needs.
  type FormEnv = AccountRepository & MasterfileRepository & StockRepository
  type Repos   = StoreRepository & FormEnv


  private def blank(company: String): Store =
    Store(
      id          = "",
      name        = "",
      description = "",
      costcenter  = "",
      account     = "-1",
      oaccount    = "-1",
      company     = company,
      stocks      = Nil)

  // `stocks` is intentionally not read from the params — the Stocks
  // sub-grid is read-only.
  private def applyParams(existing: Store, params: Map[String, String]): Store =
    existing.copy(
      id          = params.getOrElse("id_display", existing.id),
      name        = params.getOrElse("name", existing.name),
      description = params.getOrElse("description", existing.description),
      costcenter  = params.getOrElse("costcenter", existing.costcenter),
      account     = params.getOrElse("account", existing.account),
      oaccount    = params.getOrElse("oaccount", existing.oaccount))

  private def renderStore(s: Store, mode: String = "view")
  : ZIO[FormEnv, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    for {
      accs   <- AccountRepository.all((ModelId.ACCOUNT.modelid, s.company))
      ccs    <- MasterfileRepository.all((ModelId.COST_CEMTER.modelid, s.company))
      stocks <- StockRepository.getByStore(s.id, ModelId.STOCK.modelid, s.company)
    } yield StoreFormView.render(s.copy(stocks = stocks), accs, ccs, mode)

  private val listGet: Route[StoreRepository, Response] =
    Method.GET / "html" / "store-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort")
          .map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty)
          .getOrElse(HtmxTable.Page.defaultSort)

        StoreRepository
          .all((ModelId.STORE.modelid, company))
          .map { all =>
            val filtered =
              if query.isEmpty then all
              else all.filter(s =>
                s.id.contains(query) ||
                  s.name.toLowerCase.contains(query.toLowerCase))
            val sorted = filtered.sortWith { (x, y) =>
              val cmp = sort.iterator
                .map { case (key, dir) =>
                  val c = key match
                    case "name"        => x.name.trim.compareTo(y.name.trim)
                    case "description" => x.description.trim.compareTo(y.description.trim)
                    case "costcenter"  => x.costcenter.trim.compareTo(y.costcenter.trim)
                    case _             => x.id.trim.compareTo(y.id.trim)
                  if dir == "desc" then -c else c
                }
                .find(_ != 0)
                .getOrElse(0)
              cmp < 0
            }
            val slice = sorted.slice(page * size, page * size + size)
            HtmxResponse.html(
              StoreListView.render(
                HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
                company,
                query))
          }
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[AccountRepository & MasterfileRepository, Response] =
    Method.GET / "html" / "store" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        (for {
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          ccs  <- MasterfileRepository.all((ModelId.COST_CEMTER.modelid, company))
        } yield HtmxResponse.html(
          StoreFormView.render(blank(company), accs, ccs, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "store" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          s    <- StoreRepository.getById((id, modelid, company))
          html <- renderStore(s)          // mode = "view"
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "store" ->
      handler { (req: Request) =>
        HtmxEndpoint.saveForm(
          req,
          load   = (id, modelid, company) =>
            StoreRepository.getById((id, modelid, company)),
          update = (existing: Store, params) => applyParams(existing, params),
          save   = (s: Store) => StoreRepository.modify(s),
          render = (s: Store) => renderStore(s))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug