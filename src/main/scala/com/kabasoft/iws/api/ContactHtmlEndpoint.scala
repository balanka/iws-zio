package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Contact, ModelId}
import com.kabasoft.iws.repository.PartnerRepository
import zio._
import zio.http._

object ContactHtmlEndpoint:

  type Repos = PartnerRepository

  private def blank(company: String): Contact =
    Contact(
      id = "", name = "", description = "", street = "", zip = "",
      city = "", state = "", country = "", phone = "", email = "",
      company = company)

  private def renderContact(c: Contact, mode: String = "view")
  : ZIO[Any, Nothing, String] = ZIO.succeed(ContactFormView.render(c, mode))

  private def applyParams(existing: Contact, params: Map[String, String]): Contact =
    existing.copy(
      id          = params.getOrElse("id_display", existing.id),
      name        = params.getOrElse("name", existing.name),
      description = params.getOrElse("description", existing.description),
      street      = params.getOrElse("street", existing.street),
      zip         = params.getOrElse("zip", existing.zip),
      city        = params.getOrElse("city", existing.city),
      state       = params.getOrElse("state", existing.state),
      country     = params.getOrElse("country", existing.country),
      phone       = params.getOrElse("phone", existing.phone),
      email       = params.getOrElse("email", existing.email))

  private val listGet: Route[PartnerRepository, Response] =
    Method.GET / "html" / "contact-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)
        PartnerRepository.all((ModelId.CONTACT.modelid, company)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(c => c.id.contains(query) ||
              c.name.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "name"  => x.name.trim.compareTo(y.name.trim)
                case "city"  => x.city.trim.compareTo(y.city.trim)
                case "phone" => x.phone.trim.compareTo(y.phone.trim)
                case "email" => x.email.trim.compareTo(y.email.trim)
                case _       => x.id.trim.compareTo(y.id.trim)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            ContactListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[Any, Response] =
    Method.GET / "html" / "contact" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        ZIO.succeed(HtmxResponse.html(
          ContactFormView.render(blank(company), mode = "create")))
      }

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "contact" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        (for {
          c    <- PartnerRepository.getById((id, modelid, company))
          html <- renderContact(c)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "contact" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.getOrElse("id", "")
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(ModelId.CONTACT.modelid)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else PartnerRepository.getById((id, modelid, company))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then PartnerRepository.create(toSave).unit
          else PartnerRepository.modify(toSave).unit
          fresh    <- PartnerRepository.getById((toSave.id, modelid, company))
          html     <- renderContact(fresh)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug