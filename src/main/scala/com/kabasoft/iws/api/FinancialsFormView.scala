package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{
  Account, Contact, FinancialsTransaction, FinancialsTransactionDetails, Fmodule, Store
}
import HtmxFields.{comboRow, fmtDate}

object FinancialsFormView:

  // ── Line columns ────────────────────────────────────────────────────────

  private def lineColumns(accounts: List[Account]): List[HtmxSubGrid.Column] =
    val accountOptions    = accounts.map(a => a.id -> s"${a.id} ${a.name}")
    val accountOptionData = accounts.map(a => a.id -> Map("name" -> a.name)).toMap

    List(
      HtmxSubGrid.Column("id",           "Id",       "w-16", printHidden = true),
      HtmxSubGrid.Column("account",      "Account",  "w-40",
        kind       = HtmxSubGrid.CellKind.Select,
        options    = accountOptions,
        optionData = accountOptionData,
        resizable  = true),
      HtmxSubGrid.Column("accountName",  "Acc name", "",
        kind     = HtmxSubGrid.CellKind.Text,
        readonly = true,
        hidden   = true),
      HtmxSubGrid.Column("side",         "Side",     "",
        kind    = HtmxSubGrid.CellKind.Select,
        options = List("false" -> "Debit", "true" -> "Credit"),
        hidden  = true),
      HtmxSubGrid.Column("oaccount",     "Oaccount", "w-40",
        kind       = HtmxSubGrid.CellKind.Select,
        options    = accountOptions,
        optionData = accountOptionData,
        resizable  = true),
      HtmxSubGrid.Column("oaccountName", "Oacc name", "",
        kind     = HtmxSubGrid.CellKind.Text,
        readonly = true,
        hidden   = true),
      HtmxSubGrid.Column("amount",       "Amount",   "num w-24",
        kind      = HtmxSubGrid.CellKind.Money,
        resizable = true),
      HtmxSubGrid.Column("duedate",      "Due date", "w-32",
        kind      = HtmxSubGrid.CellKind.Date,
        resizable = true, printHidden = true),
      HtmxSubGrid.Column("text",         "Text",     "",
        resizable = true)
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
          HtmxFields.fmtDate(l.duedate),
          l.text),
        hidden = Map("currency" -> l.currency))
    }

  private def headerTotal(t: FinancialsTransaction): java.math.BigDecimal =
    t.lines
      .map(_.amount)
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))

  // ── Main render ─────────────────────────────────────────────────────────

  def render(
              t:                  FinancialsTransaction,
              fmodules:           List[Fmodule],
              stores:             List[Store],
              accounts:           List[Account],
              contacts:           List[Contact],
              copyFromCandidates: List[FinancialsTransaction] = Nil,
              mode:               String = "view",
              title:              String = "Header"
            ): String =
    val isCreate = mode == "create"
    val formId   = "financials-form"
    val formClass =
      if t.posted then "htmx-form financials-form form-readonly"
      else             "htmx-form financials-form"

    val currencyCode =
      t.lines.headOption.map(_.currency).filter(_.nonEmpty).getOrElse("EUR")
    val currencySymbol = MoneyFmt.symbol(currencyCode)

    val total = headerTotal(t)

    // Footer: Total only — no VAT on Financials.
    // Visible columns: 1:id  2:account  3:oaccount  4:amount  5:duedate  6:text
    val totalCell =
      s"""<span class="subgrid-foot-label">Total </span>""" +
        s"""<span class="calc-total num">${total.toString}</span>""" +
        s"""<span class="currency-suffix">${Html.esc(currencySymbol)}</span>"""

    val footers = List(
      HtmxSubGrid.FooterCell(5, "", "subgrid-sum-cell"),           // id..duedate
      HtmxSubGrid.FooterCell(1, totalCell, "num subgrid-sum-cell") // text column
    )

    val lineGrid = HtmxSubGrid.render(
      id             = s"$formId-lines",
      prefix         = "lines",
      fkField        = "transid",
      fkValue        = t.id.toString,
      columns        = lineColumns(accounts),
      rows           = lineRows(t),
      totalField     = "amount",
      currencySymbol = currencySymbol,
      footers        = footers)

    // ── Copy-from combo ────────────────────────────────────────────────
    val copyFromOptions: List[(String, String)] =
      if copyFromCandidates.isEmpty then Nil
      else
        ("" -> "— Copy from —") ::
          copyFromCandidates.map { c =>
            val label = s"${c.id}  ${c.period}  ${c.text.take(30)}".trim
            c.id.toString -> label
          }

    val copyFromCell =
      if copyFromOptions.isEmpty then
        s"""<span></span>
           |<span></span>""".stripMargin
      else
        comboRow(
          label      = "Copy from",
          id         = s"$formId-copyFrom",
          name       = "copyFrom",
          options    = copyFromOptions,
          value      = "",
          hxGet      = "/html/financials/copy-from",
          hxTarget   = s"#$formId",
          hxInclude  = s"#$formId [name='record_modelid'], #$formId [name='company']",
          hxTrigger  = "change",
          hxSwap     = "outerHTML",
          extraClass = "copy-from-select"
        )

    // ── Header section ─────────────────────────────────────────────────
    //   Row 1: Id              | Financials
    //   Row 2: Oid             | Transaction date
    //   Row 3: Cost center     | Copy from
    //   Row 4: Account         | Period
    //   Row 5: Contact         | Posted
    //   Row 6: Text            | <span>
    //   Row 7: Foot text       | <span>
    val headerSection = HtmxCollapsible.section(
      title = title,
      id    = s"$formId-sec-header",
      open  = true,
      content =
        s"""
           |${HtmxFields.textRow("Id",  s"$formId-id",  "id_display", t.id.toString, readonly = !isCreate)}
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
           |
           |${HtmxFields.textRow("Oid", s"$formId-oid", "oid", t.oid)}
           |${HtmxFields.dateRow("Transaction date", s"$formId-td", "transdate", fmtDate(t.transdate))}
           |
           |${comboRow(
          label   = "Cost center",
          id      = s"$formId-cc",
          name    = "costcenter",
          options = stores.map(s => s.id -> s"${s.id} ${s.name}"),
          value   = t.costcenter
        )}
           |$copyFromCell
           |
           |${comboRow(
          label   = "Account",
          id      = s"$formId-account",
          name    = "account",
          options = accounts.map(a => a.id -> s"${a.id} ${a.name}"),
          value   = t.account
        )}
           |${HtmxFields.textRow("Period", s"$formId-pd", "period", t.period.toString, readonly = true)}
           |
           |${comboRow(
          label   = "Contact",
          id      = s"$formId-contact",
          name    = "contact",
          options = contacts.map(c => c.id -> s"${c.id} ${c.name}"),
          value   = t.contact
        )}
           |${HtmxFields.checkboxRow("Posted", s"$formId-posted", "posted", t.posted)}
           |
           |<label class="label print-foot-label" for="$formId-text">Text</label>
           |<textarea id="$formId-text" name="text" class="textarea span-value">${Html.esc(t.text)}</textarea>
           |""".stripMargin)

    val linesBlock =
      s"""
         |<div class="subgrid-block">
         |  $lineGrid
         |</div>
         |""".stripMargin

    val footTextPrintBlock =
      s"""
         |<div class="print-foot-grid">
         |  <label class="label">Foot text</label>
         |  <div class="print-foot-box">${Html.esc(t.footText)}</div>
         |</div>
         |""".stripMargin

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
       |  $linesBlock
       |  $footTextPrintBlock
       |</form>
       |""".stripMargin