package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Article, Contact, Fmodule, Store, Transaction, Vat}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

object TransactionFormView:

  private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private def fmtDate(i: Instant): String =
    if i == null then "" else dateFmt.format(i.atZone(ZoneId.systemDefault()))

  /** Native <select> rendered as a combobox.
   *
   * @param extraClass additional CSS classes on the <select> element —
   *                   used to mark filter selectors (see `.filter-select`)
   *                   that must remain interactive in view mode.
   * The optional `hx*` params let a combo drive another region (e.g. list).
   */
  private def comboRow(
                        label:      String,
                        id:         String,
                        name:       String,
                        options:    List[(String, String)],
                        value:      String,
                        hxGet:      String = "",
                        hxTarget:   String = "",
                        hxInclude:  String = "",
                        hxTrigger:  String = "change",
                        extraClass: String = ""
                      ): String =
    val opts = options.map { case (v, lbl) =>
      val sel = if v == value then " selected" else ""
      s"""<option value="${Html.esc(v)}"$sel>${Html.esc(lbl)}</option>"""
    }.mkString

    val htmxAttrs =
      val parts = List(
        if hxGet.nonEmpty     then s"""hx-get="${Html.esc(hxGet)}""""           else "",
        if hxTarget.nonEmpty  then s"""hx-target="${Html.esc(hxTarget)}""""     else "",
        if hxInclude.nonEmpty then s"""hx-include="${Html.esc(hxInclude)}""""   else "",
        if hxGet.nonEmpty     then s"""hx-trigger="${Html.esc(hxTrigger)}""""    else ""
      ).filter(_.nonEmpty)
      if parts.isEmpty then "" else " " + parts.mkString(" ")

    val cls = List("select", extraClass).filter(_.nonEmpty).mkString(" ")

    s"""<label class="label" for="${Html.esc(id)}">${Html.esc(label)}</label>
       |<select id="${Html.esc(id)}" name="${Html.esc(name)}" class="${Html.esc(cls)}"$htmxAttrs>
       |  $opts
       |</select>""".stripMargin

  // ── Line columns ────────────────────────────────────────────────────────

  private def lineColumns(
                           articles: List[Article],
                           vats:     List[Vat]
                         ): List[HtmxSubGrid.Column] =
    List(
      HtmxSubGrid.Column("id",          "Id",       "w-16"),
      HtmxSubGrid.Column("article",     "Article",  "w-40",
        kind = HtmxSubGrid.CellKind.Select,
        options = articles.map(a => a.id -> s"${a.id} ${a.name}")),
      HtmxSubGrid.Column("articleName", "Name",     "w-40"),
      HtmxSubGrid.Column("quantity",    "Qty",      "num w-16",
        kind = HtmxSubGrid.CellKind.Number),
      HtmxSubGrid.Column("unit",        "Unit",     "w-16"),
      HtmxSubGrid.Column("price",       "Price",    "num w-24",
        kind = HtmxSubGrid.CellKind.Money),
      HtmxSubGrid.Column("amount",      "Amount",   "num w-24",
        kind = HtmxSubGrid.CellKind.Computed,
        expr = "quantity * price", readonly = true),
      HtmxSubGrid.Column("vatCode",     "VAT",      "w-20",
        kind = HtmxSubGrid.CellKind.Select,
        options = vats.map(v => v.id -> s"${v.id} ${v.name}")),
      HtmxSubGrid.Column("vat",         "VAT amt",  "num w-20",
        kind = HtmxSubGrid.CellKind.Money),
      HtmxSubGrid.Column("duedate",     "Due date", "w-32",
        kind = HtmxSubGrid.CellKind.Date),
      HtmxSubGrid.Column("text",        "Text",     "")
    )

  private def lineRows(t: Transaction): List[HtmxSubGrid.Row] =
    t.lines.map { l =>
      val amount = l.quantity.multiply(l.price)
      HtmxSubGrid.Row(
        cells = List(
          l.id.toString,
          l.article,
          l.articleName,
          l.quantity.toString,
          l.unit,
          l.price.toString,
          amount.toString,
          l.vatCode,
          l.vat.toString,
          fmtDate(l.duedate),
          l.text),
        hidden = Map(
          "transid"  -> l.transid.toString,
          "currency" -> l.currency))
    }

  private def headerTotal(t: Transaction): java.math.BigDecimal =
    t.lines
      .map(l => l.quantity.multiply(l.price).add(l.vat))
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))

  // ── Main render ─────────────────────────────────────────────────────────

  def render(
              t:        Transaction,
              fmodules: List[Fmodule],
              stores:   List[Store],
              accounts: List[Account],
              articles: List[Article],
              vats:     List[Vat],
              contacts: List[Contact],
              mode:     String = "view"
            ): String =
    val isCreate = mode == "create"
    val formId   = "transaction-form"
    val formClass =
      if mode == "view" then "htmx-form transaction-form form-readonly"
      else                     "htmx-form transaction-form"

    val lineGrid = HtmxSubGrid.render(
      id         = s"$formId-lines",
      prefix     = "lines",
      fkField    = "transid",
      fkValue    = t.id.toString,
      columns    = lineColumns(articles, vats),
      rows       = lineRows(t),
      totalField = "amount")

    val total = headerTotal(t)

    // Merged Header section — Summary + Module + Dates
    val headerSection = HtmxCollapsible.section(
      title = "Header",
      id    = s"$formId-sec-header",
      open  = true,
      content =
        s"""
           |${HtmxFields.textRow("Id",  s"$formId-id",  "id",  t.id.toString, readonly = !isCreate)}
           |${HtmxFields.textRow("Oid", s"$formId-oid", "oid", t.oid)}
           |
           |${comboRow(
          label      = "Transaction",
          id         = s"$formId-fmodule",
          name       = "modelid",
          options    = fmodules.map(f => f.id.toString -> s"${f.id} ${f.name}"),
          value      = t.modelid.toString,
          hxGet      = "/html/transaction-list",
          hxTarget   = "#transaction-list-card .card-content",
          hxInclude  = s"#$formId [name='company']",
          hxTrigger  = "change",
          extraClass = "filter-select"
        )}
           |${comboRow(
          label   = "Contact",
          id      = s"$formId-contact",
          name    = "contact",
          options = contacts.map(c => c.id -> s"${c.id} ${c.name}"),
          value   = t.contact
        )}
           |
           |${comboRow(
          label   = "Account",
          id      = s"$formId-account",
          name    = "account",
          options = accounts.map(a => a.id -> s"${a.id} ${a.name}"),
          value   = t.account
        )}
           |${comboRow(
          label   = "Store",
          id      = s"$formId-store",
          name    = "store",
          options = stores.map(s => s.id -> s"${s.id} ${s.name}"),
          value   = t.store
        )}
           |
           |${HtmxFields.textRow("Transaction date", s"$formId-td", "transdate", fmtDate(t.transdate))}
           |${HtmxFields.textRow("Period",           s"$formId-pd", "period",    t.period.toString, readonly = true)}
           |
           |${HtmxFields.checkboxRow("Posted", s"$formId-posted", "posted", t.posted)}
           |<span></span>
           |<span></span>
           |""".stripMargin)

    val textSection = HtmxCollapsible.section(
      title = "Text",
      id    = s"$formId-sec-text",
      content =
        s"""
           |${HtmxFields.textareaRow("Text",      s"$formId-text", "text",     t.text)}
           |${HtmxFields.textareaRow("Foot text", s"$formId-foot", "footText", t.footText)}
           |""".stripMargin)

    val linesSection = HtmxCollapsible.section(
      title = "Lines",
      id    = s"$formId-sec-lines",
      open  = true,
      content =
        s"""
           |<div class="subgrid-block">
           |  $lineGrid
           |  <div class="subgrid-total">
           |    <span class="label">Total:</span>
           |    <span class="calc-total num">${total.toString}</span>
           |  </div>
           |</div>
           |""".stripMargin)

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/transaction"
       |      hx-target="#$formId" hx-swap="outerHTML"
       |      hx-on::after-request="if(event.detail.requestConfig.verb === 'put' && event.detail.successful){ var c=document.getElementById('$formId-fmodule'); if(c) htmx.trigger(c,'change'); }">
       |
       |  ${HtmxFields.hiddenField("id",      t.id.toString)}
       |  ${HtmxFields.hiddenField("company", t.company)}
       |  ${HtmxFields.hiddenField("mode",    mode)}
       |
       |  $headerSection
       |  $textSection
       |  $linesSection
       |</form>
       |""".stripMargin