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
      HtmxSubGrid.Column("id",          "Id",       "w-16"),
      HtmxSubGrid.Column("article",     "Article",  "w-40",
        kind       = HtmxSubGrid.CellKind.Select,
        options    = articleOptions,
        optionData = articleOptionData),
      HtmxSubGrid.Column("articleName", "Name",     "",
        kind     = HtmxSubGrid.CellKind.Text,
        readonly = true,
        hidden   = true),
      HtmxSubGrid.Column("quantity",    "Qty",      "num w-16",
        kind = HtmxSubGrid.CellKind.Number),
      HtmxSubGrid.Column("unit",        "Unit",     "w-16",
        kind     = HtmxSubGrid.CellKind.Text,
        readonly = true),
      HtmxSubGrid.Column("price",       "Price",    "num w-24",
        kind = HtmxSubGrid.CellKind.Money),
      HtmxSubGrid.Column("amount",      "Amount",   "num w-24",
        kind     = HtmxSubGrid.CellKind.Computed,
        expr     = "quantity * price",
        readonly = true),
      HtmxSubGrid.Column("vatCode",     "VAT",      "w-24",
        kind       = HtmxSubGrid.CellKind.Select,
        options    = vats.map(v => v.id -> s"${v.id} ${v.name}"),
        optionData = vatOptionData),
      HtmxSubGrid.Column("vat",         "VAT amt",  "num w-20",
        kind     = HtmxSubGrid.CellKind.Computed,
        expr     = "quantity * price * vatRate",
        readonly = true),
      HtmxSubGrid.Column("duedate",     "Due date", "w-32",
        kind = HtmxSubGrid.CellKind.Date),
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

    val lineGrid = HtmxSubGrid.render(
      id             = s"$formId-lines",
      prefix         = "lines",
      fkField        = "transid",
      fkValue        = t.id.toString,
      columns        = lineColumns(articles, vats, t.modelid),
      rows           = lineRows(t),
      totalField     = "amount",
      currencySymbol = currencySymbol)

    val total = headerTotal(t)

    // ── Copy-from combo (row 3, right column) ──────────────────────────
    val copyFromOptions: List[(String, String)] =
      if copyFromCandidates.isEmpty then Nil
      else
        ("" -> "— Copy from —") ::
          copyFromCandidates.map { c =>
            val label = s"${c.id}  ${c.period}  ${c.text.take(30)}".trim
            c.id.toString -> label
          }

    // If there's no combo, use two empty cells so Period and Posted stay
    // in the right column of their rows.
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
           | ${HtmxFields.textRow("Oid", s"$formId-oid", "oid", t.oid)}
           |  ${HtmxFields.dateRow("Transaction date", s"$formId-td", "transdate", fmtDate(t.transdate))}
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
           |    <label class="label" for="$formId-text">Text</label>
           |     <textarea id="$formId-text" name="text" class="textarea span-value">${Html.esc(t.text)}</textarea>
           |     <label class="label" for="$formId-foot">Foot text</label>
           |    <textarea id="$formId-foot" name="footText" class="textarea span-value">${Html.esc(t.footText)}</textarea>
           |      """.stripMargin)

    val textSection = HtmxCollapsible.section(
      title = "Text",
      id    = s"$formId-sec-text",
      content =
        s"""
           |${HtmxFields.textareaRow("Text", s"$formId-text", "text", t.text)}
           |""".stripMargin)

    val linesBlock =
      s"""
         |<div class="subgrid-block">
         |  $lineGrid
         |  <div class="subgrid-total">
         |    <span class="label">Total:</span>
         |    <span class="input-money total-money">
         |      <span class="calc-total num">${total.toString}</span>
         |      <span class="currency-suffix">${Html.esc(currencySymbol)}</span>
         |    </span>
         |  </div>
         |</div>
         |""".stripMargin

    val footTextSection = HtmxCollapsible.section(
      title = "Foot text",
      id    = s"$formId-sec-foot",
      content =
        s"""
           |${HtmxFields.textareaRow("Foot text", s"$formId-foot", "footText", t.footText)}
           |""".stripMargin)

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/transaction"
       |      hx-target="#$formId" hx-swap="outerHTML"
       |
       |  ${HtmxFields.hiddenField("id",      t.id.toString)}
       |  ${HtmxFields.hiddenField("company", t.company)}
       |  ${HtmxFields.hiddenField("mode",    mode)}
       |  ${HtmxFields.hiddenField("record_modelid", t.modelid.toString)}
       |
       |  $headerSection
       |  $linesBlock
       |</form>
       |""".stripMargin