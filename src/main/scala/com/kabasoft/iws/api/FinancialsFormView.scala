package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{
  Account, Contact, FinancialsTransaction, FinancialsTransactionDetails, Fmodule, Store
}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneId}

object FinancialsFormView:

  private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  private def fmtDate(i: Instant): String =
    if i == null then "" else dateFmt.format(i.atZone(ZoneId.systemDefault()))

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
  private def lineColumns(accounts: List[Account]): List[HtmxSubGrid.Column] =
    val accountOptions = accounts.map(a => a.id -> s"${a.id} ${a.name}")
    val accountOptionData = accounts.map(a => a.id -> Map("name" -> a.name)).toMap

    List(
      HtmxSubGrid.Column("id", "Id", "w-16"),
      HtmxSubGrid.Column("account", "Account", "w-40",
        kind = HtmxSubGrid.CellKind.Select,
        options = accountOptions,
        optionData = accountOptionData),
      HtmxSubGrid.Column("accountName", "Acc name", "", // ← hidden
        kind = HtmxSubGrid.CellKind.Text,
        readonly = true,
        hidden = true),
      HtmxSubGrid.Column("side", "Side", "w-20",
        kind = HtmxSubGrid.CellKind.Select,
        options = List("false" -> "Debit", "true" -> "Credit")),
      HtmxSubGrid.Column("oaccount", "Oaccount", "w-40",
        kind = HtmxSubGrid.CellKind.Select,
        options = accountOptions,
        optionData = accountOptionData),
      HtmxSubGrid.Column("oaccountName", "Oacc name", "", // ← hidden
        kind = HtmxSubGrid.CellKind.Text,
        readonly = true,
        hidden = true),
      HtmxSubGrid.Column("amount", "Amount", "num w-24",
        kind = HtmxSubGrid.CellKind.Money),
      HtmxSubGrid.Column("duedate", "Due date", "w-32",
        kind = HtmxSubGrid.CellKind.Date),
      HtmxSubGrid.Column("text", "Text", "")
    )

  private def lineRows(t: FinancialsTransaction): List[HtmxSubGrid.Row] =
    t.lines.map { l =>
      HtmxSubGrid.Row(
        cells = List(
          l.id.toString,
          l.account,
          l.accountName,
          l.side.toString,
          l.oaccount,
          l.oaccountName,
          l.amount.toString,
          fmtDate(l.duedate),
          l.text),
        hidden = Map("currency" -> l.currency))
    }

  private def headerTotal(t: FinancialsTransaction): java.math.BigDecimal =
    t.lines
      .map(_.amount)
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))

  // ── Main render ─────────────────────────────────────────────────────────

  def render(
              t:        FinancialsTransaction,
              fmodules: List[Fmodule],
              stores:   List[Store],
              accounts: List[Account],
              contacts: List[Contact],
              mode:     String = "view"
            ): String =
    val isCreate = mode == "create"
    val formId   = "financials-form"
    val formClass =
      if mode == "view" then "htmx-form financials-form form-readonly"
      else                     "htmx-form financials-form"

    val currencyCode =
      t.lines.headOption.map(_.currency).filter(_.nonEmpty).getOrElse("EUR")
    val currencySymbol = MoneyFmt.symbol(currencyCode)

    val lineGrid = HtmxSubGrid.render(
      id = s"$formId-lines",
      prefix = "lines",
      fkField = "transid",
      fkValue = t.id.toString,
      columns = lineColumns(accounts),
      rows = lineRows(t),
      totalField = "amount",
      currencySymbol = currencySymbol)

    val total = headerTotal(t)

    val headerSection = HtmxCollapsible.section(
      title = "Header",
      id    = s"$formId-sec-header",
      open  = true,
      content =
        s"""
           |${HtmxFields.textRow("Id",  s"$formId-id",  "id_display", t.id.toString, readonly = !isCreate)}
           |${HtmxFields.textRow("Oid", s"$formId-oid", "oid",        t.oid)}
           |
           |${comboRow(
          label      = "Financials",
          id         = s"$formId-fmodule",
          name       = "modelid",
          options    = fmodules.map(f => f.id.toString -> s"${f.id} ${f.name}"),
          value      = t.modelid.toString,
          hxGet      = "/html/financials-list",
          hxTarget   = "#financials-list-card .card-content",
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
          label   = "Cost center",
          id      = s"$formId-cc",
          name    = "costcenter",
          options = stores.map(s => s.id -> s"${s.id} ${s.name}"),
          value   = t.costcenter
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
           |       span class="input-money total-money">
           |       <span class="calc-total num">${total.toString}</span>
           |       <span class="currency-suffix">${Html.esc(currencySymbol)}</span>
           |    </span>
           |  </div>
           |</div>
           |""".stripMargin)

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/financials"
       |      hx-target="#$formId" hx-swap="outerHTML"
       |      hx-on::after-request="if(event.detail.requestConfig.verb === 'put' && event.detail.successful){ var c=document.getElementById('$formId-fmodule'); if(c) htmx.trigger(c,'change'); }">
       |
       |  ${HtmxFields.hiddenField("id",           t.id.toString)}
       |  ${HtmxFields.hiddenField("company",      t.company)}
       |  ${HtmxFields.hiddenField("mode",         mode)}
       |  ${HtmxFields.hiddenField("record_modelid", t.modelid.toString)}
       |
       |  $headerSection
       |  $textSection
       |  $linesSection
       |</form>
       |""".stripMargin