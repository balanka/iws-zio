package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Article, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, VatRepository}
import zio._
import zio.http._
import java.math.BigDecimal

object ArticleHtmlEndpoint:

  // The masterfile lists the form needs alongside the article repository.
  type FormEnv = AccountRepository & VatRepository
  type Repos   = ArticleRepository & FormEnv

  private def blank(company: String): Article =
    Article(
      id = "",
      name = "",
      description = "",
      parent = "",
      sprice = BigDecimal("0.00"),
      pprice = BigDecimal("0.00"),
      avgPrice = BigDecimal("0.00"),
      currency = "EUR",
      stocked = false,
      quantityUnit = "",
      packUnit = "",
      account = "-1",
      oaccount = "-1",
      revenueAccount = "-1",
      vatCode = "-1",
      company = company
    )
  // ── Form renderer ───────────────────────────────────────────────────────
  private def renderArticle(a: Article)
  : ZIO[FormEnv, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    for {
      accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, a.company))
      vats <- VatRepository.all((ModelId.VAT.modelid, a.company))
    } yield ArticleFormView.render(a, accs, vats)

  // ── List handler ────────────────────────────────────────────────────────

  private val listGet: Route[ArticleRepository, Response] =
    Method.GET / "html" / "article-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort")
          .map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty)
          .getOrElse(HtmxTable.Page.defaultSort)

        ArticleRepository
          .all((ModelId.ARTICLE.modelid, company))
          .map { all =>
            val filtered =
              if query.isEmpty then all
              else all.filter(a =>
                a.id.contains(query) ||
                  a.name.toLowerCase.contains(query.toLowerCase)
              )
            val sorted = filtered.sortWith { (x, y) =>
              val cmp = sort.iterator
                .map { case (key, dir) =>
                  val c = key match
                    case "name"        => x.name.trim.compareTo(y.name.trim)
                    case "description" => x.description.trim.compareTo(y.description.trim)
                    case "sprice"      => x.sprice.compareTo(y.sprice)
                    case _             => x.id.trim.compareTo(y.id.trim)
                  if dir == "desc" then -c else c
                }
                .find(_ != 0)
                .getOrElse(0)
              cmp < 0
            }
            val slice = sorted.slice(page * size, page * size + size)
            HtmxResponse.html(
              ArticleListView.render(
                HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
                company,
                query
              )
            )
          }
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  // ── Form handlers ───────────────────────────────────────────────────────

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "article" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          a    <- ArticleRepository.getById((id, modelid, company))
          html <- renderArticle(a)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "article" ->
      handler { (req: Request) =>
        HtmxEndpoint.saveForm(
          req,
          load = (id, modelid, company) =>
            ArticleRepository.getById((id, modelid, company)),
          update = (existing: Article, params) => existing.copy(
            name           = params.getOrElse("name", existing.name),
            description    = params.getOrElse("description", existing.description),
            parent         = params.getOrElse("parent", existing.parent),
            sprice         = BigDecimal(params.getOrElse("sprice", existing.sprice.toString)),
            pprice         = BigDecimal(params.getOrElse("pprice", existing.pprice.toString)),
            avgPrice       = BigDecimal(params.getOrElse("avgPrice", existing.avgPrice.toString)),
            currency       = params.getOrElse("currency", existing.currency),
            quantityUnit   = params.getOrElse("quantityUnit", existing.quantityUnit),
            packUnit       = params.getOrElse("packUnit", existing.packUnit),
            account        = params.getOrElse("account", existing.account),
            oaccount       = params.getOrElse("oaccount", existing.oaccount),
            revenueAccount = params.getOrElse("revenueAccount", existing.revenueAccount),
            vatCode        = params.getOrElse("vatCode", existing.vatCode),
            stocked        = params.contains("stocked")
          ),
          save   = (a: Article) => ArticleRepository.modify(a),
          render = (a: Article) => renderArticle(a)
        )
      }
  private val formNew: Route[FormEnv, Response] =
    Method.GET / "html" / "article" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        (for {
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          vats <- VatRepository.all((ModelId.VAT.modelid, company))
        } yield HtmxResponse.html(
          ArticleFormView.render(blank(company), accs, vats, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }
  // ── Assembly ────────────────────────────────────────────────────────────

  val routes: Routes[Repos, Response] =
    Routes(listGet, formGet, formPut) @@ Middleware.debug