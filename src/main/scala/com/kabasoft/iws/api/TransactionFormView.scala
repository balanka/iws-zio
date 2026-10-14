package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Article, Contact, Fmodule, Store, Transaction, Vat, ModelId}
import HtmxFields.{comboRow, fmtDate}

object TransactionFormView:

  // ── Line columns ────────────────────────────────────────────────────────

  private def lineColumns(
                           articles: List[Article],
                           vats:     List[Vat],
                           modelid:  Int
                         ): List[HtmxSubGrid.Column] =
    val isSales = Set(
      ModelId.SALES_ORDER.modelid,
      ModelId.SALES_INVOICE.modelid,
      ModelId.SALES_QUOTATION.modelid,
      ModelId.SALES_CONTRACT.modelid,
      ModelId.BILL_OF_DELIVERY.modelid
    ).contains(modelid)

    val articleOptions: List[(String, String)] =
      articles.map(a => a.id -> s"${a.id} ${a.name}")

    val articleOptionData: Map[String, Map[String, String]] =
      articles.map { a =>
        val price = if isSales then a.sprice else a.pprice
        a.id -> Map(
          "name"    -> a.name,
          "unit"    -> a.quantityUnit,
          "price"   -> price.toString,
          "vatcode" -> a.vatCode
        )
      }.toMap

    val vatOptionData: Map[String, Map[String, String]] =
      vats.map(v => v.id -> Map("rate" -> v.percent.toString)).toMap

    List(
      HtmxSubGrid.Column("id",          "Id",       "w-16", printHidden = true),
      HtmxSubGrid.Column("article",     "Article",  "w-40",
        kind       = HtmxSubGrid.CellKind.Select,
        options    = articleOptions,
        optionData = articleOptionData,
        resizable  = true),
      HtmxSubGrid.Column("articleName", "Name",     "",
        kind     = HtmxSubGrid.CellKind.Text,
        readonly = true,
        hidden   = true),
      HtmxSubGrid.Column("quantity",    "Qty",      "num w-16",
        kind      = HtmxSubGrid.CellKind.Number,
        resizable = true),
      HtmxSubGrid.Column("unit",        "Unit",     "w-16",
        kind      = HtmxSubGrid.CellKind.Text,
        readonly  = true,
        resizable = true),
      HtmxSubGrid.Column("price",       "Price",    "num w-24",
        kind      = HtmxSubGrid.CellKind.Money,
        resizable = true),
      HtmxSubGrid.Column("amount",      "Amount",   "num w-24",
        kind      = HtmxSubGrid.CellKind.Computed,
        expr      = "quantity * price",
        readonly  = true,
        resizable = true),
      HtmxSubGrid.Column("vatCode",     "VAT",      "w-24",
        kind       = HtmxSubGrid.CellKind.Select,
        options    = vats.map(v => v.id -> s"${v.id} ${v.name}"),
        optionData = vatOptionData,
        resizable  = true),
      HtmxSubGrid.Column("vat",         "VAT amt",  "num w-20",
        kind      = HtmxSubGrid.CellKind.Computed,
        expr      = "quantity * price * vatRate",
        readonly  = true,
        resizable = true),
      HtmxSubGrid.Column("duedate",     "Due date", "w-32",
        kind      = HtmxSubGrid.CellKind.Date,
        resizable = true, printHidden = true),
      HtmxSubGrid.Column("text",        "Text",     "",
        resizable = true)
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
          HtmxFields.fmtDate(l.duedate),
          l.text),
        hidden = Map("currency" -> l.currency))
    }

  private def headerTotal(t: Transaction): java.math.BigDecimal =
    t.lines
      .map(l => l.quantity.multiply(l.price).add(l.vat))
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))

  private def vatTotal(t: Transaction): java.math.BigDecimal =
    t.lines
      .map(_.vat)
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))

  // ── Main render ─────────────────────────────────────────────────────────

  def render(
              t:                  Transaction,
              fmodules:           List[Fmodule],
              stores:             List[Store],
              accounts:           List[Account],
              articles:           List[Article],
              vats:               List[Vat],
              contacts:           List[Contact],
              copyFromCandidates: List[Transaction] = Nil,
              mode:               String = "view",
              title:              String = "Header"
            ): String =
    val isCreate = mode == "create"
    val formId   = "transaction-form"
    val formClass =
      if t.posted then "htmx-form transaction-form form-readonly"
      else             "htmx-form transaction-form"

    val currencyCode =
      t.lines.headOption.map(_.currency).filter(_.nonEmpty).getOrElse("EUR")
    val currencySymbol = MoneyFmt.symbol(currencyCode)

    val vatSum = vatTotal(t)
    val total  = headerTotal(t)

    // Footer layout — visible columns of lineColumns:
    //   1:id  2:article  3:quantity  4:unit  5:price  6:amount
    //   7:vatCode  8:vat  9:duedate  10:text
    //
    // One label per cell. `Vat:` sits left of the VAT sum, `Total ` sits
    // left of the total, both right-aligned within their columns.
    val vatSumCell =
      s"""<span class="subgrid-foot-label">Vat: </span>""" +
        s"""<span class="calc-vat-total num">${vatSum.toString}</span>""" +
        s"""<span class="currency-suffix">${Html.esc(currencySymbol)}</span>"""

    val totalCell =
      s"""<span class="subgrid-foot-label">Total </span>""" +
        s"""<span class="calc-total num">${total.toString}</span>""" +
        s"""<span class="currency-suffix">${Html.esc(currencySymbol)}</span>"""

    val footers = List(
      HtmxSubGrid.FooterCell(1, "", "subgrid-sum-cell print-hide"),   // id
      HtmxSubGrid.FooterCell(6, "", "subgrid-sum-cell"),              // article..vatCode
      HtmxSubGrid.FooterCell(1, vatSumCell, "num subgrid-sum-cell"),  // vat (VAT amt)
      HtmxSubGrid.FooterCell(1, "", "subgrid-sum-cell print-hide"),   // duedate
      HtmxSubGrid.FooterCell(1, totalCell, "num subgrid-sum-cell")    // text
    )


    val lineGrid = HtmxSubGrid.render(
      id             = s"$formId-lines",
      prefix         = "lines",
      fkField        = "transid",
      fkValue        = t.id.toString,
      columns        = lineColumns(articles, vats, t.modelid),
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
          hxGet      = "/html/transaction/copy-from",
          hxTarget   = s"#$formId",
          hxInclude  = s"#$formId [name='record_modelid'], #$formId [name='company']",
          hxTrigger  = "change",
          hxSwap     = "outerHTML",
          extraClass = "copy-from-select"
        )

    // ── Header section ─────────────────────────────────────────────────
    //   Row 1: Id              | Transaction
    //   Row 2: Oid             | Transaction date
    //   Row 3: Store           | Copy from
    //   Row 4: Account         | Period
    //   Row 5: Contact         | Posted
    //   Row 6: Text            | <span>
    //   Row 7: Foot text       | <span>  (printed copy is at the bottom)
    val headerSection = HtmxCollapsible.section(
      title = title,
      id    = s"$formId-sec-header",
      open  = true,
      content =
        s"""
           |${HtmxFields.textRow("Id",  s"$formId-id",  "id_display", t.id.toString, readonly = !isCreate)}
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
           |
           |${HtmxFields.textRow("Oid", s"$formId-oid", "oid", t.oid)}
           |${HtmxFields.dateRow("Transaction date", s"$formId-td", "transdate", fmtDate(t.transdate))}
           |
           |${comboRow(
          label   = "Store",
          id      = s"$formId-store",
          name    = "store",
          options = stores.map(s => s.id -> s"${s.id} ${s.name}"),
          value   = t.store
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

    // Printed foot text — rendered after the subgrid so it lands at
    // the bottom of the printed page. Hidden on screen.
    //   <div class="print-foot-box">${Html.esc(t.footText)}</div>
    //<div class="print-foot-grid">
    //<textarea id="$formId-text" name="text" class="textarea span-value">${Html.esc(t.footText)}</textarea>
    //</div>
    val footTextPrintBlock =
      s"""
         |<div class="print-foot-grid">
         |<textarea id="$formId-text" name="text" class="textarea span-value">${Html.esc(t.footText)}</textarea>
         |</div>
         |""".stripMargin
    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/transaction"
       |      hx-target="#$formId" hx-swap="outerHTML"
       |      hx-on::after-request="if(event.detail.requestConfig.verb === 'put' && event.detail.successful){ var c=document.getElementById('$formId-fmodule'); if(c) htmx.trigger(c,'change'); }">
       |
       |  ${HtmxFields.hiddenField("id",      t.id.toString)}
       |  ${HtmxFields.hiddenField("company", t.company)}
       |  ${HtmxFields.hiddenField("mode",    mode)}
       |  ${HtmxFields.hiddenField("record_modelid", t.modelid.toString)}
       |
       |  $headerSection
       |  $linesBlock
       |  $footTextPrintBlock
       |</form>
       |""".stripMargin