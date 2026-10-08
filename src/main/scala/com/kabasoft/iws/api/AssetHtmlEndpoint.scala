package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Asset, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, AssetRepository}
import zio._
import zio.http._
import java.math.BigDecimal

object AssetHtmlEndpoint:

  type Repos = AssetRepository & AccountRepository

  private def blank(company: String): Asset =
    Asset(
      id = "", name = "", description = "",
      company = company, account = "-1", oaccount = "-1",
      scrapValue = BigDecimal("1.00"), lifeSpan = 1, depMethod = 1,
      amount = BigDecimal("0.00"), rate = BigDecimal("0.00"),
      frequency = 1, currency = "EUR")

  private def renderAsset(a: Asset, mode: String = "view")
  : ZIO[AccountRepository, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    AccountRepository.all((ModelId.ACCOUNT.modelid, a.company))
      .map(accounts => AssetFormView.render(a, accounts, mode))

  private def applyParams(existing: Asset, params: Map[String, String]): Asset =
    existing.copy(
      id          = params.getOrElse("id_display", existing.id),
      name        = params.getOrElse("name", existing.name),
      description = params.getOrElse("description", existing.description),
      currency    = params.getOrElse("currency", existing.currency),
      account     = params.getOrElse("account", existing.account),
      oaccount    = params.getOrElse("oaccount", existing.oaccount),
      amount      = BigDecimal(params.getOrElse("amount", existing.amount.toString)),
      scrapValue  = BigDecimal(params.getOrElse("scrapValue", existing.scrapValue.toString)),
      rate        = BigDecimal(params.getOrElse("rate", existing.rate.toString)),
      lifeSpan    = params.get("lifeSpan").flatMap(_.toIntOption).getOrElse(existing.lifeSpan),
      frequency   = params.get("frequency").flatMap(_.toIntOption).getOrElse(existing.frequency),
      depMethod   = params.get("depMethod").flatMap(_.toIntOption).getOrElse(existing.depMethod))

  private val listGet: Route[AssetRepository, Response] =
    Method.GET / "html" / "asset-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)
        AssetRepository.all((ModelId.ASSET.modelid, company)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(a => a.id.contains(query) ||
              a.name.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "name"        => x.name.trim.compareTo(y.name.trim)
                case "description" => x.description.trim.compareTo(y.description.trim)
                case "amount"      => x.amount.compareTo(y.amount)
                case _             => x.id.trim.compareTo(y.id.trim)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            AssetListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[AccountRepository, Response] =
    Method.GET / "html" / "asset" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          .map(accs => HtmxResponse.html(
            AssetFormView.render(blank(company), accs, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "asset" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          a    <- AssetRepository.getById((id, modelid, company))
          html <- renderAsset(a)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "asset" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.getOrElse("id", "")
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(ModelId.ASSET.modelid)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else AssetRepository.getById((id, modelid, company))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then AssetRepository.create(toSave).unit
          else AssetRepository.modify(toSave).unit
          fresh    <- AssetRepository.getById((toSave.id, modelid, company))
          html     <- renderAsset(fresh)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug