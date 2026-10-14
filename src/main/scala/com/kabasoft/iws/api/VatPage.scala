package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Vat}
import com.kabasoft.iws.repository.{AccountRepository, VatRepository}
import zio._
import zio.http._

object VatPage:

  private val pageGet: Route[VatRepository & AccountRepository, Response] =
    Method.GET / "vat-htmx" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        HtmxEndpoint.loadPage(
          "VAT", id, modelid, company,
          load   = (id, modelid, company) => VatRepository.getById((id, modelid, company)),
          render = (vat: Vat) =>
            AccountRepository.all((ModelId.ACCOUNT.modelid, vat.company))
              .map(accs => VatFormView.render(vat, accs))
        )
      }

  val routes: Routes[VatRepository & AccountRepository, Response] = Routes(pageGet)
