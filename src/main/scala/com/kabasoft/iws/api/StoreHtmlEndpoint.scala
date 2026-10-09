package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Stock, Store}
import com.kabasoft.iws.repository.{AccountRepository, MasterfileRepository, StoreRepository}
import java.math.BigDecimal
import zio._
import zio.http._

object StoreHtmlEndpoint:

  type Repos   = StoreRepository & AccountRepository & MasterfileRepository
  type FormEnv = AccountRepository & MasterfileRepository

  private def blank(company: String): Store =
    Store(
      id = "", name = "", description = "", costcenter = "",
      account = "-1", oaccount = "-1", company = company, stocks = Nil)

  private def renderStore(s: Store, mode: String = "view")
  : ZIO[FormEnv, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    for {
      accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, s.company))
      ccs  <- MasterfileRepository.all((ModelId.COST_CEMTER.modelid, s.company))
    } yield StoreFormView.render(s, accs, ccs, mode)

  private def applyParams(existing: Store, params: Map[String, String]): Store =
    val parentId = params.getOrElse("id_display", existing.id)

    val stocks: List[Stock] =
      HtmxFormIndex.parseIndexed(params, "stocks").map { m =>
        Stock(
          id = m.getOrElse("id", ""),
          store = parentId,
          article = m.getOrElse("article", ""),
          quantity = BigDecimal(m.getOrElse("quantity", "0")),
          price = BigDecimal(m.getOrElse("price", "0")),
          charge = m.getOrElse("charge", ""),
          company = existing.company,
          modelid = ModelId.STOCK.modelid)
      }

    existing.copy(
      id = parentId,
      name = params.getOrElse("name", existing.name),
      description = params.getOrElse("description", existing.description),
      costcenter = params.getOrElse("costcenter", existing.costcenter),
      account = params.getOrElse("account", existing.account),
      oaccount = params.getOrElse("oaccount", existing.oaccount),
      stocks = stocks)

//  private def applyParams(existing: Store, params: Map[String, String]): Store =
//    existing.copy(
//      id          = params.getOrElse("id_display", existing.id),
//      name        = params.getOrElse("name", existing.name),
//      description = params.getOrElse("description", existing.description),
//      costcenter  = params.getOrElse("costcenter", existing.costcenter),
//      account     = params.getOrElse("account", existing.account),
//      oaccount    = params.getOrElse("oaccount", existing.oaccount))

  private val listGet: Route[StoreRepository, Response] =
    Method.GET / "html" / "store-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)
        StoreRepository.all((ModelId.STORE.modelid, company)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(s => s.id.contains(query) ||
              s.name.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "name"        => x.name.trim.compareTo(y.name.trim)
                case "description" => x.description.trim.compareTo(y.description.trim)
                case "costcenter"  => x.costcenter.trim.compareTo(y.costcenter.trim)
                case _             => x.id.trim.compareTo(y.id.trim)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            StoreListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[FormEnv, Response] =
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
          html <- renderStore(s)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "store" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.getOrElse("id", "")
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(ModelId.STORE.modelid)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else StoreRepository.getById((id, modelid, company))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then StoreRepository.create(toSave).unit
          else StoreRepository.modify(toSave).unit
          fresh    <- StoreRepository.getById((toSave.id, modelid, company))
          html     <- renderStore(fresh)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug