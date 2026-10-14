package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Transaction, TransactionDetails, Vat}
import com.kabasoft.iws.repository.{
  AccountRepository, ArticleRepository, FModuleRepository,
  PartnerRepository, StoreRepository, TransactionRepository, VatRepository
}
import com.kabasoft.iws.service.TransactionService
import zio._
import zio.http._
import java.time.{Instant, LocalDate, ZoneId}

object TransactionHtmlEndpoint:

  type FormEnv =
    AccountRepository & ArticleRepository & FModuleRepository &
      PartnerRepository & StoreRepository & TransactionRepository &
      TransactionService & VatRepository

  // ── Params → entity ────────────────────────────────────────────────────

  private def applyParams(
                           base:   Transaction,
                           params: Map[String, String],
                           vats:   List[Vat]
                         ): Transaction =
    val transDate: Instant =
      params.get("transdate").filter(_.nonEmpty)
        .map(s => LocalDate.parse(s).atStartOfDay(ZoneId.systemDefault()).toInstant)
        .getOrElse(base.transdate)

    val ld     = transDate.atZone(ZoneId.systemDefault()).toLocalDate
    val period = ld.getYear * 100 + ld.getMonthValue

    def rateFor(vatCode: String): java.math.BigDecimal =
      vats.find(_.id == vatCode).map(_.percent).getOrElse(java.math.BigDecimal.ZERO)

    val lines: List[TransactionDetails] =
      HtmxFormIndex.parseIndexed(params, "lines").map { m =>
        val qty   = new java.math.BigDecimal(m.getOrElse("quantity", "0"))
        val price = new java.math.BigDecimal(m.getOrElse("price",    "0"))
        val code  = m.getOrElse("vatCode", "-1")
        val rate  = rateFor(code)
        val vat   = qty.multiply(price).multiply(rate)
        val due: Instant =
          m.get("duedate").filter(_.nonEmpty)
            .map(s => LocalDate.parse(s).atStartOfDay(ZoneId.systemDefault()).toInstant)
            .getOrElse(transDate)

        TransactionDetails(
          id          = m.getOrElse("id", "-1").toLong,
          transid     = m.getOrElse("transid", base.id.toString).toLong,
          article     = m.getOrElse("article",     ""),
          articleName = m.getOrElse("articleName", ""),
          quantity    = qty,
          unit        = m.getOrElse("unit",        ""),
          price       = price,
          currency    = m.getOrElse("currency",    "EUR"),
          duedate     = due,
          vatCode     = code,
          vat         = vat,
          text        = m.getOrElse("text",        ""),
          company     = base.company,
          modelid     = base.modelid)
      }

    base.copy(
      id        = params.get("id_display").flatMap(_.toLongOption)
        .orElse(params.get("id").flatMap(_.toLongOption))
        .getOrElse(base.id),
      oid       = params.getOrElse("oid",      base.oid),
      modelid   = base.modelid,
      period    = period,
      transdate = transDate,
      store     = params.getOrElse("store",    base.store),
      account   = params.getOrElse("account",  base.account),
      contact   = params.getOrElse("contact",  base.contact),
      text      = params.getOrElse("text",     base.text),
      footText  = params.getOrElse("footText", base.footText),
      posted    = params.get("posted").exists(v => v == "true" || v == "on" || v == "1"),
      lines     = lines
    )

  private def blank(company: String, modelid: Int): Transaction =
    val ld = LocalDate.now()
    Transaction(
      id = -1L, oid = "", contact = "", store = "", account = "-1",
      transdate = Instant.now(), enterdate = Instant.now(), postingdate = Instant.now(),
      period = ld.getYear * 100 + ld.getMonthValue,
      posted = false, modelid = modelid,
      company = company, text = "", footText = "", lines = Nil)

  // ── Shared loaders ─────────────────────────────────────────────────────

  private def loadLists(company: String) =
    for {
      accs     <- AccountRepository.all((ModelId.ACCOUNT.modelid, company))
      arts     <- ArticleRepository.all((ModelId.ARTICLE.modelid, company))
      vats     <- VatRepository.all((ModelId.VAT.modelid, company))
      stores   <- StoreRepository.all((ModelId.STORE.modelid, company))
      contacts <- PartnerRepository.all((ModelId.CONTACT.modelid, company))
      fmodules <- FModuleRepository.all((ModelId.FMODULE.modelid, company))
    } yield FormData(
      accounts = accs,
      articles = arts,
      vats     = vats,
      stores   = stores,
      contacts = contacts,
      fmodules = fmodules)

  private def loadCandidates(company: String, modelid: Int)
  : ZIO[TransactionRepository, Nothing, List[Transaction]] =
    if modelid <= 0 || company.isEmpty then ZIO.succeed(Nil)
    else
      TransactionRepository.all((modelid, company))
        .map(_.sortBy(-_.id).take(50))
        .catchAll(_ => ZIO.succeed(Nil))

  // ── Routes ─────────────────────────────────────────────────────────────

  private val formNew: Route[FormEnv, Response] =
    Method.GET / "html" / "transaction" / "new-form" ->
      handler { (req: Request) =>
        val modelid = req.queryParam("modelid").flatMap(_.toIntOption).getOrElse(-1)
        val company = req.queryParam("company").getOrElse("")
        (for {
          data       <- loadLists(company)
          candidates <- loadCandidates(company, modelid)
        } yield HtmxResponse.html(
          TransactionFormView.render(
            t                  = blank(company, modelid),
            fmodules           = data.fmodules,
            stores             = data.stores,
            accounts           = data.accounts,
            articles           = data.articles,
            vats               = data.vats,
            contacts           = data.contacts,
            copyFromCandidates = candidates,
            mode               = "create")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formGet: Route[FormEnv, Response] =
    Method.GET / "html" / "transaction" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        id.toLongOption match
          case Some(lid) =>
            (for {
              t          <- TransactionRepository.getById((lid, modelid, company))
              data       <- loadLists(company)
              candidates <- loadCandidates(company, modelid)
            } yield HtmxResponse.html(
              TransactionFormView.render(
                t                  = t,
                fmodules           = data.fmodules,
                stores             = data.stores,
                accounts           = data.accounts,
                articles           = data.articles,
                vats               = data.vats,
                contacts           = data.contacts,
                copyFromCandidates = candidates,
                mode               = "view")))
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case None => ZIO.succeed(Response.status(Status.BadRequest))
      }

  private val formCopyFrom: Route[FormEnv, Response] =
    Method.GET / "html" / "transaction" / "copy-from" ->
      handler { (req: Request) =>
        val srcId   = req.queryParam("copyFrom").flatMap(_.toLongOption)
        val modelid = req.queryParam("record_modelid").flatMap(_.toIntOption).getOrElse(-1)
        val company = req.queryParam("company").getOrElse("")

        (srcId, modelid, company) match
          case (Some(sid), m, c) if m > 0 && c.nonEmpty =>
            (for {
              cloned     <- TransactionService.copyFrom(sid, m, m, c)
              data       <- loadLists(c)
              candidates <- loadCandidates(c, m)
            } yield HtmxResponse.htmlWithTrigger(
                TransactionFormView.render(
                  t                  = cloned,
                  fmodules           = data.fmodules,
                  stores             = data.stores,
                  accounts           = data.accounts,
                  articles           = data.articles,
                  vats               = data.vats,
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
    Method.PUT / "html" / "transaction" ->
      handler { (req: Request) =>
        (for {
          body <- req.body.asString
          params = HtmxResponse.parseForm(body)

          rawMode = params.getOrElse("mode", "view")
          company = params.getOrElse("company", "").trim

          _ <- if company.isEmpty then
            ZIO.fail(new RuntimeException("Missing 'company' in form submission"))
          else ZIO.unit

          // A positive id is the only valid signal for "modify an existing row".
          lookupId =
            params.get("id_display").flatMap(_.toLongOption).filter(_ > 0)
              .orElse(params.get("id").flatMap(_.toLongOption).filter(_ > 0))

          // Prefer record_modelid (immutable). Fall back to the combo's modelid.
          modelid0 =
            params.get("record_modelid").flatMap(_.toIntOption).filter(_ > 0)
              .orElse(params.get("modelid").flatMap(_.toIntOption).filter(_ > 0))

          modelid = modelid0.getOrElse(ModelId.TRANSACTION.modelid)

          // Effective mode: "create" if the form said so OR we have no valid
          // id to look up.
          mode = if rawMode == "create" || lookupId.isEmpty then "create" else "view"

          data <- loadLists(company)

          existing <- if mode == "create" then ZIO.succeed(blank(company, modelid))
          else
            lookupId match
              case Some(lid) =>
                TransactionRepository
                  .getById((lid, modelid, company))
                  .catchAll(_ => ZIO.succeed(blank(company, modelid)))
              case None =>
                ZIO.succeed(blank(company, modelid))

          toSave = applyParams(existing, params, data.vats)

          // create returns the fully persisted entity (with its new positive id).
          // modify returns a row count, so reload after modify.
          fresh  <- if mode == "create" then TransactionRepository.create(toSave)
          else
            for {
              _ <- TransactionRepository.modify(toSave)
              t <- TransactionRepository
                .getById((toSave.id, modelid, company))
                .catchAll(_ =>
                  ZIO.fail(new RuntimeException(
                    s"Modified transaction id=${toSave.id} modelid=$modelid company=$company, but it could not be reloaded")))
            } yield t

          candidates <- loadCandidates(company, modelid)
        } yield HtmxResponse.htmlWithTrigger(
          TransactionFormView.render(
            t                  = fresh,
            fmodules           = data.fmodules,
            stores             = data.stores,
            accounts           = data.accounts,
            articles           = data.articles,
            vats               = data.vats,
            contacts           = data.contacts,
            copyFromCandidates = candidates,
            mode               = "view"),
          "iws:list-refresh"))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  /** Column filters — one `case` per column that has a filter.
   * Empty string means "no filter applied".
   */
  private def matches(
                       t: Transaction,
                       key: String,
                       value: String
                     ): Boolean =
    if value.isEmpty then true
    else
      val v = value.trim.toLowerCase
      key match
        case "id" => t.id.toString.contains(v)
        case "oid" => t.oid.toLowerCase.contains(v)
        case "modelid" => t.modelid.toString.contains(v)
        case "period" => t.period.toString.contains(v)
        case "contact" => t.contact.toLowerCase.contains(v)
        case "account" => t.account.toLowerCase.contains(v)
        case "store" => t.store.toLowerCase.contains(v)
        case "text" => t.text.toLowerCase.contains(v)
        case "total" =>
          val tot = t.lines.map(_.amount)
            .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))
          tot.toString.contains(v)
        case "posted" => t.posted.toString == value
        case _ => true

  private val listGet: Route[FormEnv, Response] =
    Method.GET / "html" / "transaction-list" ->
      handler { (req: Request) =>
        val modelid = req.queryParam("modelid").flatMap(_.toIntOption).getOrElse(-1)
        val company = req.queryParam("company").getOrElse("")
        val page = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort = req.queryParam("sort")
          .map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty)
          .getOrElse(HtmxTable.Page.defaultSort)

        val filterKeys = List(
          "id", "oid", "modelid", "period",
          "contact", "account", "store", "text", "total", "posted")
        val filters: Map[String, String] =
          filterKeys.flatMap(k => req.queryParam(k).map(k -> _)).toMap

        if modelid < 0 || company.isEmpty then
          ZIO.succeed(HtmxResponse.html(
            """<div class="p-4 text-center text-gray-500">Select a transaction type above.</div>"""))
        else
          TransactionRepository.all((modelid, company))
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
                TransactionListView.render(
                  page = HtmxTable.Page(slice, page, size, filtered.size.toLong, sort),
                  company = company,
                  query = query,
                  filterValues = filters))
            }
            .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }
  private val formPrint: Route[FormEnv, Response] =
    Method.GET / "html" / "transaction" / "print" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        id.toLongOption match
          case Some(lid) =>
            (for {
              t <- TransactionRepository.getById((lid, modelid, company))
              data <- loadLists(company)
            } yield {
              val fmoduleName = data.fmodules.find(_.id == t.modelid).map(_.name).getOrElse("Transaction")
              val docTitle = s"$company/$fmoduleName"
              HtmxResponse.html(
                HtmxPrintPage.render(
                  docTitle,
                  TransactionFormView.render(
                    t = t,
                    fmodules = data.fmodules,
                    stores = data.stores,
                    accounts = data.accounts,
                    articles = data.articles,
                    vats = data.vats,
                    contacts = data.contacts,
                    mode = "view",
                    title = docTitle)))
            })
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case None =>
            ZIO.succeed(Response.status(Status.BadRequest))
      }

  val routes: Routes[FormEnv, Response] =
    Routes(formNew, formGet, formCopyFrom, formPut, listGet, formPrint) @@ Middleware.debug