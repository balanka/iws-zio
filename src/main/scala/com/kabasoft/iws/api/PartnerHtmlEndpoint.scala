package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Contact
import com.kabasoft.iws.repository.PartnerRepository
import zio._
import zio.http._

object PartnerHtmlEndpoint:

  private val formGet: Route[PartnerRepository, Response] =
    Method.GET / "html" / "partner" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        HtmxEndpoint.loadForm(
          id, modelid, company,
          load   = (id, modelid, company) => PartnerRepository.getById((id, modelid, company)),
          render = (p: Contact) => ZIO.succeed(PartnerFormView.render(p))
        )
      }

  private val formPut: Route[PartnerRepository, Response] =
    Method.PUT / "html" / "partner" ->
      handler { (req: Request) =>
        HtmxEndpoint.saveForm(
          req,
          load   = (id, modelid, company) => PartnerRepository.getById((id, modelid, company)),
          update = (existing: Contact, params) => existing.copy(
            name        = params.getOrElse("name", existing.name),
            description = params.getOrElse("description", existing.description),
            street      = params.getOrElse("street", existing.street),
            zip         = params.getOrElse("zip", existing.zip),
            city        = params.getOrElse("city", existing.city),
            state       = params.getOrElse("state", existing.state),
            country     = params.getOrElse("country", existing.country),
            phone       = params.getOrElse("phone", existing.phone),
            email       = params.getOrElse("email", existing.email)
          ),
          save   = (p: Contact) => PartnerRepository.modify(p),
          render = (p: Contact) => ZIO.succeed(PartnerFormView.render(p))
        )
      }

  val routes: Routes[PartnerRepository, Response] =
    Routes(formGet, formPut) @@ Middleware.debug