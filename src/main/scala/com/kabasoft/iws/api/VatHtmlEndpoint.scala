package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Vat}
import com.kabasoft.iws.repository.{AccountRepository, VatRepository}
import zio._
import zio.http._

object VatHtmlEndpoint:

  private def renderVat(vat: Vat): ZIO[AccountRepository, com.kabasoft.iws.domain.AppError.RepositoryError, String] =
    AccountRepository.all((ModelId.ACCOUNT.modelid, vat.company))
      .map(accs => VatFormView.render(vat, accs))

  private val formGet: Route[VatRepository & AccountRepository, Response] =
    Method.GET / "html" / "vat" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        HtmxEndpoint.loadForm(
          id, modelid, company,
          load   = (id, modelid, company) => VatRepository.getById((id, modelid, company)),
          render = (vat: Vat) => renderVat(vat)
        )
      }

  private val formPut: Route[VatRepository & AccountRepository, Response] =
    Method.PUT / "html" / "vat" ->
      handler { (req: Request) =>
        HtmxEndpoint.saveForm(
          req,
          load   = (id, modelid, company) => VatRepository.getById((id, modelid, company)),
          update = (existing: Vat, params) => existing.copy(
            name             = params.getOrElse("name", existing.name),
            description      = params.getOrElse("description", existing.description),
            percent          = java.math.BigDecimal(params.getOrElse("percent", existing.percent.toString)),
            inputVatAccount  = params.getOrElse("inputVatAccount", existing.inputVatAccount),
            outputVatAccount = params.getOrElse("outputVatAccount", existing.outputVatAccount)
          ),
          save   = (vat: Vat) => VatRepository.modify(vat),
          render = (vat: Vat) => renderVat(vat)
        )
      }

  val routes: Routes[VatRepository & AccountRepository, Response] =
    Routes(formGet, formPut) @@ Middleware.debug