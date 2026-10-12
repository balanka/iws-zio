package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{
  FinancialsTransaction, FinancialsTransactionDetails, ModelId
}
import com.kabasoft.iws.repository.{
  AccountRepository, FModuleRepository, FinancialsTransactionRepository,
  PartnerRepository, StoreRepository
}
import zio._
import zio.http._
import java.time.{Instant, LocalDate, ZoneId}

object FinancialsHtmlEndpoint:

  type FormEnv =
    AccountRepository & FModuleRepository & FinancialsTransactionRepository &
      PartnerRepository & StoreRepository

  private def bd(s: String, fallback: String = "0"): java.math.BigDecimal =
    Option(s).map(_.trim).filter(v =>
      v.nonEmpty && v != "null" && v != "undefined"
    ) match
      case Some(v) => new java.math.BigDecimal(v)
      case None    => new java.math.BigDecimal(fallback)

  private def applyParams(
                           base:   FinancialsTransaction,
                           params: Map[String, String]
                         ): FinancialsTransaction =
    val transDate: Instant =
      params.get("transdate").filter(_.nonEmpty)
        .map(s => LocalDate.parse(s).atStartOfDay(ZoneId.systemDefault()).toInstant)
        .getOrElse(base.transdate)

    val ld     = transDate.atZone(ZoneId.systemDefault()).toLocalDate
    val period = ld.getYear * 100 + ld.getMonthValue

    val lines: List[FinancialsTransactionDetails] =
      HtmxFormIndex.parseIndexed(params, "lines")
        .filterNot(_.values.forall(v => v == null || v.trim.isEmpty))
        .map { m =>
          val amount = bd(m.getOrElse("amount", "0"))
          val side   = m.get("side").exists(v => v == "true" || v == "1")
          val due: Instant =
            Option(m.getOrElse("duedate", "")).filter(_.nonEmpty)
              .map(s => LocalDate.parse(s).atStartOfDay(ZoneId.systemDefault()).toInstant)
              .getOrElse(transDate)

          FinancialsTransactionDetails(
            id           = bd(m.getOrElse("id", "-1")).longValue(),
            transid      = bd(m.getOrElse("transid", base.id.toString)).longValue(),
            account      = Option(m.getOrElse("account", "")).getOrElse(""),
            side         = side,
            oaccount     = Option(m.getOrElse("oaccount", "")).getOrElse(""),
            amount       = amount,
            duedate      = due,
            text         = Option(m.getOrElse("text", "")).getOrElse(""),
            currency     = Option(m.getOrElse("currency", "EUR")).getOrElse("EUR"),
            company      = base.company,
            accountName  = Option(m.getOrElse("accountName", "")).getOrElse(""),
            oaccountName = Option(m.getOrElse("oaccountName", "")).getOrElse(""),
            modelid      = base.modelid)                    // ← inherits parent
        }

    base.copy(
      id         = params.get("id_display").flatMap(_.toLongOption)
        .orElse(params.get("id").flatMap(_.toLongOption))
        .getOrElse(base.id),
      oid        = params.getOrElse("oid",         base.oid),
      modelid    = base.modelid,                             // ← immutable
      period     = period,
      transdate  = transDate,
      contact    = params.getOrElse("contact",     base.contact),
      costcenter = params.getOrElse("costcenter",  base.costcenter),
      account    = params.getOrElse("account",     base.account),
      text       = params.getOrElse("text",        base.text),
      footText   = params.getOrElse("footText",    base.footText),
      posted     = params.get("posted").exists(v => v == "true" || v == "on" || v == "1"),
      lines      = lines
    )

  private def blank(company: String, modelid: Int): FinancialsTransaction =
    val ld = LocalDate.now()
    FinancialsTransaction(
      id          = 0L,
      oid         = "",
      contact     = "",
      costcenter  = "",
      account     = "-1",
      transdate   = Instant.now(),
      enterdate   = Instant.now(),
      postingdate = Instant.now(),
      period      = ld.getYear * 100 + ld.getMonthValue,
      posted      = false,
      modelid     = modelid,
      company     = company,
      text        = "",
      footText    = "",
      lines       = Nil)

  private def loadLists(company: String) =
    for {
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      stores   <- StoreRepository.all((ModelId.STORE.modelid, company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fmodules <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
    } yield (accs, stores, contacts, fmodules)

  // ── Routes ─────────────────────────────────────────────────────────────

  private val formNew: Route[FormEnv, Response] =
    Method.GET / "html" / "financials" / "new-form" ->
      handler { (req: Request) =>
        val modelid = req.queryParam("modelid").flatMap(_.toIntOption).getOrElse(-1)
        val company = req.queryParam("company").getOrElse("")
        (for {
          lists <- loadLists(company)
          (accs, stores, contacts, fmodules) = lists
        } yield HtmxResponse.html(
          FinancialsFormView.render(
            t        = blank(company, modelid),
            fmodules = fmodules,
            stores   = stores,
            accounts = accs,
            contacts = contacts,
            mode     = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet: Route[FormEnv, Response] =
    Method.GET / "html" / "financials" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        id.toLongOption match
          case Some(lid) =>
            (for {
              t     <- FinancialsTransactionRepository.getById((lid, modelid, company))
              lists <- loadLists(company)
              (accs, stores, contacts, fmodules) = lists
            } yield HtmxResponse.html(
              FinancialsFormView.render(
                t        = t,
                fmodules = fmodules,
                stores   = stores,
                accounts = accs,
                contacts = contacts,
                mode     = "view")))
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case None =>
            ZIO.succeed(Response.status(Status.BadRequest))
      }

  private val formPut: Route[FormEnv, Response] =
    Method.PUT / "html" / "financials" ->
      handler { (req: Request) =>
        (for {
          body   <- req.body.asString
          params  = HtmxResponse.parseForm(body)
          mode    = params.getOrElse("mode", "view")
          company = params.getOrElse("company", "")

          modelid = params.get("record_modelid").flatMap(_.toIntOption)
            .filter(_ > 0)
            .orElse(params.get("modelid").flatMap(_.toIntOption))
            .getOrElse(ModelId.FINANCIALS.modelid)

          existing <- if mode == "create" then ZIO.succeed(blank(company, modelid))
          else
            val lookupId =
              params.get("id_display").flatMap(_.toLongOption)
                .orElse(params.get("id").flatMap(_.toLongOption))
            lookupId match
              case Some(lid) => FinancialsTransactionRepository.getById((lid, modelid, company))
              case None      => ZIO.succeed(blank(company, modelid))

          toSave = applyParams(existing, params)

          _      <- if mode == "create" then FinancialsTransactionRepository.create(toSave)
          else FinancialsTransactionRepository.modify(toSave)

          fresh  <- FinancialsTransactionRepository.getById((toSave.id, modelid, company))
          lists  <- loadLists(company)
          (accs, stores, contacts, fmodules) = lists
        } yield HtmxResponse.html(
          FinancialsFormView.render(
            t        = fresh,
            fmodules = fmodules,
            stores   = stores,
            accounts = accs,
            contacts = contacts,
            mode     = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  /** Column filters — one `case` per column that has a filter.
   * Empty string means "no filter applied".
   */
  private def matches(
                       t:     FinancialsTransaction,
                       key:   String,
                       value: String
                     ): Boolean =
    if value.isEmpty then true
    else
      val v = value.trim.toLowerCase
      key match
        case "id"         => t.id.toString.contains(v)
        case "oid"        => t.oid.toLowerCase.contains(v)
        case "modelid"    => t.modelid.toString.contains(v)
        case "period"     => t.period.toString.contains(v)
        case "contact"    => t.contact.toLowerCase.contains(v)
        case "costcenter" => t.costcenter.toLowerCase.contains(v)
        case "text"       => t.text.toLowerCase.contains(v)
        case "total"      =>
          val tot = t.lines.map(_.amount)
            .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))
          tot.toString.contains(v)
        case "posted"     => t.posted.toString == value
        case _            => true

  private val listGet: Route[FormEnv, Response] =
    Method.GET / "html" / "financials-list" ->
      handler { (req: Request) =>
        val modelid = req.queryParam("modelid").flatMap(_.toIntOption).getOrElse(-1)
        val company = req.queryParam("company").getOrElse("")
        val page    = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query   = req.queryParam("q").getOrElse("")
        val size    = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort    = req.queryParam("sort")
          .map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty)
          .getOrElse(HtmxTable.Page.defaultSort)

        // Every column key that has a filter input on the client.
        val filterKeys = List("id", "oid", "modelid", "period", "contact", "costcenter",  "total", "posted", "text")
        val filters: Map[String, String] =
          filterKeys.flatMap(k => req.queryParam(k).map(k -> _)).toMap

        if modelid < 0 || company.isEmpty then
          ZIO.succeed(HtmxResponse.html(
            """<div class="p-4 text-center text-gray-500">Select a financials type above.</div>"""))
        else
          FinancialsTransactionRepository.all((modelid, company))
            .map { all =>
              val filtered =
                all
                  .filter(t => query.isEmpty ||
                    t.id.toString.contains(query) ||
                    t.oid.toLowerCase.contains(query.toLowerCase) ||
                    t.text.toLowerCase.contains(query.toLowerCase))
                  .filter(t => filters.forall { case (k, v) => matches(t, k, v) })

              val slice = filtered.slice(page * size, page * size + size)
              HtmxResponse.html(
                FinancialsListView.render(
                  page         = HtmxTable.Page(slice, page, size, filtered.size.toLong, sort),
                  company      = company,
                  query        = query,
                  filterValues = filters))
            }
            .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[FormEnv, Response] =
    Routes(formNew, formGet, formPut, listGet) @@ Middleware.debug