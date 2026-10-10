package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Asset, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, AssetRepository}
import zio._
import zio.http._
import java.math.BigDecimal

object AssetHtmlEndpoint:

  private def blank(company: String): Asset =
    Asset(
      id = "", name = "", description = "",
      company = company, account = "-1", oaccount = "-1",
      scrapValue = BigDecimal("1.00"), lifeSpan = 1, depMethod = 1,
      amount = BigDecimal("0.00"), rate = BigDecimal("0.00"),
      frequency = 1, currency = "EUR")

  private def applyParams(base: Asset, params: Map[String, String]): Asset =
    base.copy(
      id          = params.getOrElse("id_display", base.id),
      name        = params.getOrElse("name",        base.name),
      description = params.getOrElse("description", base.description),
      account     = params.getOrElse("account",     base.account),
      oaccount    = params.getOrElse("oaccount",    base.oaccount),
      currency    = params.getOrElse("currency",    base.currency),
      scrapValue  = BigDecimal(params.getOrElse("scrapValue", base.scrapValue.toString)),
      amount      = BigDecimal(params.getOrElse("amount",     base.amount.toString)),
      rate        = BigDecimal(params.getOrElse("rate",       base.rate.toString)),
      lifeSpan    = params.get("lifeSpan").flatMap(_.toIntOption).getOrElse(base.lifeSpan),
      depMethod   = params.get("depMethod").flatMap(_.toIntOption).getOrElse(base.depMethod),
      frequency   = params.get("frequency").flatMap(_.toIntOption).getOrElse(base.frequency)
    )

  private val formNew
  : Route[AccountRepository, Response] =
    Method.GET / "html" / "asset" / "new-form" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        (for {
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
        } yield HtmxResponse.html(
          AssetFormView.render(blank(company), accs, mode = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet
  : Route[AssetRepository & AccountRepository, Response] =
    Method.GET / "html" / "asset" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          a    <- AssetRepository.getById((id, modelid, company))
          accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
        } yield HtmxResponse.html(
          AssetFormView.render(a, accs, mode = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut
  : Route[AssetRepository & AccountRepository, Response] =
    Method.PUT / "html" / "asset" ->
      handler { (req: Request) =>
        (for {
          body     <- req.body.asString
          params    = HtmxResponse.parseForm(body)
          mode      = params.getOrElse("mode", "view")
          company   = params.getOrElse("company", "")
          modelid   = params.get("modelid").flatMap(_.toIntOption)
            .getOrElse(ModelId.ASSET.modelid)

          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else AssetRepository.getById((params.getOrElse("id", ""), modelid, company))
          toSave    = applyParams(existing, params)

          _        <- if mode == "create" then AssetRepository.create(toSave)
          else AssetRepository.modify(toSave)

          fresh    <- AssetRepository.getById((toSave.id, modelid, company))
          accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
        } yield HtmxResponse.html(
          AssetFormView.render(fresh, accs, mode = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[AssetRepository & AccountRepository, Response] =
    Routes(formNew, formGet, formPut) @@ Middleware.debug