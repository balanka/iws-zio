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
      id = 0L, oid = "", contact = "", store = "", account = "-1",
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
            } yield HtmxResponse.html(
              TransactionFormView.render(
                t                  = cloned,
                fmodules           = data.fmodules,
                stores             = data.stores,
                accounts           = data.accounts,
                articles           = data.articles,
                vats               = data.vats,
                contacts           = data.contacts,
                copyFromCandidates = candidates,
                mode               = "create")))
              .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
          case _ =>
            ZIO.succeed(Response.status(Status.BadRequest))
      }

  private val formPut: Route[FormEnv, Response] =
    Method.PUT / "html" / "transaction" ->
      handler { (req: Request) =>
        (for {
          body   <- req.body.asString
          params  = HtmxResponse.parseForm(body)
          mode    = params.getOrElse("mode", "view")
          company = params.getOrElse("company", "")
          modelid = params.get("record_modelid").flatMap(_.toIntOption)
            .filter(_ > 0)
            .orElse(params.get("modelid").flatMap(_.toIntOption))
            .getOrElse(ModelId.TRANSACTION.modelid)

          data   <- loadLists(company)

          existing <- if mode == "create" then ZIO.succeed(blank(company, modelid))
          else
            val lookupId =
              params.get("id_display").flatMap(_.toLongOption)
                .orElse(params.get("id").flatMap(_.toLongOption))
            lookupId match
              case Some(lid) => TransactionRepository.getById((lid, modelid, company))
              case None      => ZIO.succeed(blank(company, modelid))

          toSave = applyParams(existing, params, data.vats)

          _      <- if mode == "create" then TransactionRepository.create(toSave)
          else TransactionRepository.modify(toSave)

          fresh  <- TransactionRepository
            .getById((toSave.id, modelid, company))
            .catchAll(_ =>
              ZIO.fail(new RuntimeException(
                s"Saved transaction id=${toSave.id} modelid=$modelid company=$company, but it could not be reloaded")))

          candidates <- loadCandidates(company, modelid)
        } yield HtmxResponse.html(
          TransactionFormView.render(
            t                  = fresh,
            fmodules           = data.fmodules,
            stores             = data.stores,
            accounts           = data.accounts,
            articles           = data.articles,
            vats               = data.vats,
            contacts           = data.contacts,
            copyFromCandidates = candidates,
            mode               = "view")))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val listGet: Route[FormEnv, Response] =
    Method.GET / "html" / "transaction-list" ->
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

        if modelid < 0 || company.isEmpty then
          ZIO.succeed(HtmxResponse.html(
            """<div class="p-4 text-center text-gray-500">Select a transaction type above.</div>"""))
        else
          TransactionRepository.all((modelid, company))
            .map { all =>
              val filtered =
                if query.isEmpty then all
                else all.filter(t =>
                  t.id.toString.contains(query) ||
                    t.oid.toLowerCase.contains(query.toLowerCase) ||
                    t.text.toLowerCase.contains(query.toLowerCase))
              val slice = filtered.slice(page * size, page * size + size)
              HtmxResponse.html(
                TransactionListView.render(
                  HtmxTable.Page(slice, page, size, filtered.size.toLong, sort),
                  company, query))
            }
            .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[FormEnv, Response] =
    Routes(formNew, formGet, formCopyFrom, formPut, listGet) @@ Middleware.debug