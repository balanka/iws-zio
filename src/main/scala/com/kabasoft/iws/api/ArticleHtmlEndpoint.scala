package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Article, ModelId}
import com.kabasoft.iws.repository.{
  AccountRepository, ArticleRepository, VatRepository
}
import zio._
import zio.http._
import java.math.BigDecimal

object ArticleHtmlEndpoint:

  // ── Blank template ─────────────────────────────────────────────────────
  // Article has no `blank(...)` companion factory, so build the defaults
  // inline. Adjust the numeric / currency defaults to match your model.

  private def blank(company: String): Article =Article.dummy.copy(company = company)

  // ── Params → entity ────────────────────────────────────────────────────
  private def applyParams(base: Article, params: Map[String, String]): Article =
    base.copy(
      id             = params.getOrElse("id_display",   base.id),
      name           = params.getOrElse("name",         base.name),
      description    = params.getOrElse("description",  base.description),
      parent         = params.getOrElse("parent",       base.parent),
      sprice         = java.math.BigDecimal(params.getOrElse("sprice",   base.sprice.toString)),
      pprice         = java.math.BigDecimal(params.getOrElse("pprice",   base.pprice.toString)),
      avgPrice       = java.math.BigDecimal(params.getOrElse("avgPrice", base.avgPrice.toString)),
      currency       = params.getOrElse("currency",     base.currency),
      stocked        = params.get("stocked").exists(v => v == "true" || v == "on" || v == "1"),
      quantityUnit   = params.getOrElse("quantityUnit", base.quantityUnit),
      packUnit       = params.getOrElse("packUnit",     base.packUnit),
      account        = params.getOrElse("account",      base.account),
      oaccount       = params.getOrElse("oaccount",     base.oaccount),
      revenueAccount = params.getOrElse("revenueAccount", base.revenueAccount),
      vatCode        = params.getOrElse("vatCode",      base.vatCode),
      company        = params.getOrElse("company",      base.company)
      // modelid, enterdate, changedate, postingdate, bom, stocks
      // are server-controlled — leave them alone.
    )

  // ── New (blank form in create mode) ────────────────────────────────────

  private val formNew
  : Route[AccountRepository & VatRepository, Response] =
    Method.GET / "html" / "article" / "new-form" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        (for {
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          vats <- VatRepository.all((ModelId.VAT.modelid, company))
        } yield HtmxResponse.html(
          ArticleFormView.render(blank(company), accs, vats, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  // ── GET by id (read-only view) ─────────────────────────────────────────

  private val formGet
  : Route[ArticleRepository & AccountRepository & VatRepository, Response] =
    Method.GET / "html" / "article" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          // 3-tuple key: (id, modelid, company)
          a    <- ArticleRepository.getById((id, modelid, company))
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          vats <- VatRepository.all((ModelId.VAT.modelid, company))
        } yield HtmxResponse.html(
          ArticleFormView.render(a, accs, vats, mode = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  // ── PUT — create or modify ─────────────────────────────────────────────

  private val formPut
  : Route[ArticleRepository & AccountRepository & VatRepository, Response] =
    Method.PUT / "html" / "article" ->
      handler { (req: Request) =>
        (for {
          body     <- req.body.asString
          params    = HtmxResponse.parseForm(body)
          mode      = params.getOrElse("mode", "view")
          company   = params.getOrElse("company", "")
          modelid   = params.get("modelid").flatMap(_.toIntOption)
            .getOrElse(ModelId.ARTICLE.modelid)

          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else ArticleRepository.getById(
            (params.getOrElse("id", ""), modelid, company))

          toSave    = applyParams(existing, params)

          // create() / modify() receive the FULL entity
          _        <- if mode == "create" then ArticleRepository.create(toSave)
          else ArticleRepository.modify(toSave)

          // 3-tuple key here too
          fresh    <- ArticleRepository.getById((toSave.id, modelid, company))
          accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
          vats     <- VatRepository.all((ModelId.VAT.modelid, company))
        } yield HtmxResponse.html(
          ArticleFormView.render(fresh, accs, vats, mode = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[
    ArticleRepository & AccountRepository & VatRepository, Response
  ] = Routes(formNew, formGet, formPut) @@ Middleware.debug