package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Contact
import com.kabasoft.iws.repository.PartnerRepository
import zio._
import zio.http._

object PartnerPage:

  private val pageGet: Route[PartnerRepository, Response] =
    Method.GET / "partner-htmx" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        HtmxEndpoint.loadPage(
          "Partner", id, modelid, company,
          load   = (id, modelid, company) => PartnerRepository.getById((id, modelid, company)),
          render = (p: Contact) => ZIO.succeed(PartnerFormView.render(p))
        )
      }

  val routes: Routes[PartnerRepository, Response] = Routes(pageGet)