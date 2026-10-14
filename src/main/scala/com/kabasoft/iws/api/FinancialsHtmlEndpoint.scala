package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{
  FinancialsTransaction, FinancialsTransactionDetails, ModelId
}
import com.kabasoft.iws.repository.{
  AccountRepository, FModuleRepository, FinancialsRepository,
  PartnerRepository, StoreRepository
}
import com.kabasoft.iws.service.FinancialsService
import zio._
import zio.http._
import java.time.{Instant, LocalDate, ZoneId}

object FinancialsHtmlEndpoint:

  type FormEnv =
    AccountRepository & FModuleRepository & FinancialsRepository &
      FinancialsService & PartnerRepository & StoreRepository

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
            modelid      = base.modelid)
        }

    base.copy(
      id         = params.get("id_display").flatMap(_.toLongOption)
        .orElse(params.get("id").flatMap(_.toLongOption))
        .getOrElse(base.id),
      oid        = params.getOrElse("oid",         base.oid),
      modelid    = base.modelid,
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
      id = -1L, oid = "", contact = "", costcenter = "", account = "-1",
      transdate = Instant.now(), enterdate = Instant.now(), postingdate = Instant.now(),
      period = ld.getYear * 100 + ld.getMonthValue,
      posted = false, modelid = modelid,
      company = company, text = "", footText = "", lines = Nil)

  // ── Loaders ────────────────────────────────────────────────────────────

  private def loadLists(company: String) =
    for {
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      stores   <- StoreRepository.all((ModelId.STORE.modelid, company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fmodules <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
    } yield FormData(
      accounts = accs,
      stores   = stores,
      contacts = contacts,
      fmodules = fmodules)

  private def loadCandidates(company: String, modelid: Int)
  : ZIO[FinancialsRepository, Nothing, List[FinancialsTransaction]] =
    if modelid <= 0 || company.isEmpty then ZIO.succeed(Nil)
    else
      FinancialsRepository.all((modelid, company))
        .map(_.sortBy(-_.id).take(50))
        .catchAll(_ => ZIO.succeed(Nil))

  // ── Routes ─────────────────────────────────────────────────────────────

  private val formNew: Route[FormEnv, Response] =
    Method.GET / "html" / "financials" / "new-form" ->
      handler { (req: Request) =>
        val modelid = req.queryParam("modelid").flatMap(_.toIntOption).getOrElse(-1)
        val company = req.queryParam("company").getOrElse("")
        (for {
          data       <- loadLists(company)
          candidates <- loadCandidates(company, modelid)
        } yield HtmxResponse.html(
          FinancialsFormView.render(
            t                  = blank(company, modelid),
            fmodules           = data.fmodules,
            stores             = data.stores,
            accounts           = data.accounts,
            contacts           = data.contacts,
            copyFromCandidates = candidates,
            mode               = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet: Route[FormEnv, Response] =
    Method.GET / "html" / "financials" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        id.toLongOption match
          case Some(lid) =>
            (for {
              t          <- FinancialsRepository.getById((lid, modelid, company))
              data       <- loadLists(company)
              candidates <- loadCandidates(company, modelid)
            } yield HtmxResponse.html(
              FinancialsFormView.render(
                t                  = t,
                fmodules           = data.fmodules,
                stores             = data.stores,
                accounts           = data.accounts,
                contacts           = data.contacts,
                copyFromCandidates = candidates,
                mode               = "view")))
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case None =>
            ZIO.succeed(Response.status(Status.BadRequest))
      }

  private val formCopyFrom: Route[FormEnv, Response] =
    Method.GET / "html" / "financials" / "copy-from" ->
      handler { (req: Request) =>
        val srcId   = req.queryParam("copyFrom").flatMap(_.toLongOption)
        val modelid = req.queryParam("record_modelid").flatMap(_.toIntOption).getOrElse(-1)
        val company = req.queryParam("company").getOrElse("")

        (srcId, modelid, company) match
          case (Some(sid), m, c) if m > 0 && c.nonEmpty =>
            (for {
              cloned     <- FinancialsService.copyFrom(sid, m, m, c)
              data       <- loadLists(c)
              candidates <- loadCandidates(c, m)
            } yield HtmxResponse.htmlWithTrigger(
              FinancialsFormView.render(
                t                  = cloned,
                fmodules           = data.fmodules,
                stores             = data.stores,
                accounts           = data.accounts,
                contacts           = data.contacts,
                copyFromCandidates = candidates,
                mode               = "create"),
              "iws:list-refresh")
              )
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case _ =>
            ZIO.succeed(Response.status(Status.BadRequest))
      }
  private val formPut: Route[FormEnv, Response] =
    Method.PUT / "html" / "financials" ->
      handler { (req: Request) =>
        (for {
          body <- req.body.asString
          params = HtmxResponse.parseForm(body)

          rawMode = params.getOrElse("mode", "view")
          company = params.getOrElse("company", "").trim

          _ <- if company.isEmpty then
            ZIO.fail(new RuntimeException("Missing 'company' in form submission"))
          else ZIO.unit

          lookupId =
            params.get("id_display").flatMap(_.toLongOption).filter(_ > 0)
              .orElse(params.get("id").flatMap(_.toLongOption).filter(_ > 0))

          modelid0 =
            params.get("record_modelid").flatMap(_.toIntOption).filter(_ > 0)
              .orElse(params.get("modelid").flatMap(_.toIntOption).filter(_ > 0))

          modelid = modelid0.getOrElse(ModelId.FINANCIALS.modelid)

          mode = if rawMode == "create" || lookupId.isEmpty then "create" else "view"

          data <- loadLists(company)

          existing <- if mode == "create" then ZIO.succeed(blank(company, modelid))
          else
            lookupId match
              case Some(lid) =>
                FinancialsRepository
                  .getById((lid, modelid, company))
                  .catchAll(_ => ZIO.succeed(blank(company, modelid)))
              case None =>
                ZIO.succeed(blank(company, modelid))

          toSave = applyParams(existing, params)

          fresh  <- if mode == "create" then FinancialsRepository.create(toSave)
          else
            for {
              _ <- FinancialsRepository.modify(toSave)
              t <- FinancialsRepository
                .getById((toSave.id, modelid, company))
                .catchAll(_ =>
                  ZIO.fail(new RuntimeException(
                    s"Modified financials id=${toSave.id} modelid=$modelid company=$company, but it could not be reloaded")))
            } yield t

          candidates <- loadCandidates(company, modelid)
        } yield HtmxResponse.htmlWithTrigger(
          FinancialsFormView.render(
            t                  = fresh,
            fmodules           = data.fmodules,
            stores             = data.stores,
            accounts           = data.accounts,
            contacts           = data.contacts,
            copyFromCandidates = candidates,
            mode               = "view"),
          "iws:list-refresh"))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

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

        val filterKeys = List(
          "id", "oid", "modelid", "period",
          "contact", "costcenter", "text", "total", "posted")
        val filters: Map[String, String] =
          filterKeys.flatMap(k => req.queryParam(k).map(k -> _)).toMap

        if modelid < 0 || company.isEmpty then
          ZIO.succeed(HtmxResponse.html(
            """<div class="p-4 text-center text-gray-500">Select a financials type above.</div>"""))
        else
          FinancialsRepository.all((modelid, company))
            .map { all =>
              val filtered =
                all
                  .filter(t =>
                    query.isEmpty ||
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
  private val formPrint: Route[FormEnv, Response] =
    Method.GET / "html" / "financials" / "print" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        id.toLongOption match
          case Some(lid) =>
            (for {
              t <- FinancialsRepository.getById((lid, modelid, company))
              data <- loadLists(company)
            } yield {
              val fmoduleName = data.fmodules.find(_.id == t.modelid).map(_.name).getOrElse("Financials")
              val docTitle = s"$company/$fmoduleName"
              HtmxResponse.html(
                HtmxPrintPage.render(
                  docTitle,
                  FinancialsFormView.render(
                    t = t,
                    fmodules = data.fmodules,
                    stores = data.stores,
                    accounts = data.accounts,
                    contacts = data.contacts,
                    mode = "view",
                    title = docTitle)))
            })
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case None =>
            ZIO.succeed(Response.status(Status.BadRequest))
      }

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

  val routes: Routes[FormEnv, Response] =
    Routes(formNew, formGet, formCopyFrom, formPut, listGet, formPrint) @@ Middleware.debug