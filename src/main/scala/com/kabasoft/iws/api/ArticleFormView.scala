package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Article, Stock, Vat}
import java.math.BigDecimal

object ArticleFormView:

  private val stockColumns = List(
    HtmxSubGrid.Column("id",       "Id",     "w-24"),
    HtmxSubGrid.Column("store",    "Store",  "w-28"),
    HtmxSubGrid.Column("quantity", "Qty",    "num w-20"),
    HtmxSubGrid.Column("price",    "Price",  "num w-24"),
    HtmxSubGrid.Column("charge",   "Charge", "w-20")
  )

  private def stockRows(a: Article): List[HtmxSubGrid.Row] =
    a.stocks.map(s => HtmxSubGrid.Row(
      cells = List(
        s.id,
        s.store,
        s.quantity.toString,
        s.price.toString,
        s.charge)))

  def render(
              a:        Article,
              accounts: List[Account],
              vats:     List[Vat],
              mode:     String = "view"
            ): String =
    val isCreate  = mode == "create"
    val formClass =
      if mode == "view" then "htmx-form article-form form-readonly"
      else                     "htmx-form article-form"
    val formId = "article-form"

    val stockGrid = HtmxSubGrid.render(
      id       = s"$formId-stocks",
      prefix   = "stocks",
      fkField  = "article",
      fkValue  = a.id,
      columns  = stockColumns,
      rows     = stockRows(a),
      readonly = true)

    // ── Sections ────────────────────────────────────────────────────────

    val summarySection = HtmxCollapsible.section(
      title = "Summary",
      id    = s"$formId-sec-summary",
      open  = true,
      content =
        s"""
           |${HtmxFields.textRow("Id",   s"$formId-id",   "id_display", a.id, readonly = !isCreate)}
           |${HtmxFields.textRow("Name", s"$formId-name", "name", a.name)}
           |${HtmxFields.textareaRow("Description", s"$formId-desc", "description", a.description)}
           |${HtmxFields.textRow("Parent", s"$formId-parent", "parent", a.parent)}
           |""".stripMargin)

    val pricingSection = HtmxCollapsible.section(
      title = "Pricing",
      id    = s"$formId-sec-pricing",
      content =
        s"""
           |${HtmxFields.moneyRow("Sales price",    s"$formId-sprice",   "sprice",   a.sprice.toString,   a.currency)}
           |${HtmxFields.moneyRow("Purchase price", s"$formId-pprice",   "pprice",   a.pprice.toString,   a.currency)}
           |${HtmxFields.moneyRow("Average price",  s"$formId-avgPrice", "avgPrice", a.avgPrice.toString, a.currency)}
           |${HtmxFields.currencyRow("Currency",    s"$formId-currency", "currency", a.currency)}
           |""".stripMargin)

    val unitsSection = HtmxCollapsible.section(
      title = "Units",
      id    = s"$formId-sec-units",
      content =
        s"""
           |${HtmxFields.textRow("Quantity unit", s"$formId-qtyUnit",  "quantityUnit", a.quantityUnit)}
           |${HtmxFields.textRow("Pack unit",     s"$formId-packUnit", "packUnit",     a.packUnit)}
           |${HtmxFields.checkboxRow("Stocked",   s"$formId-stocked",  "stocked",      a.stocked)}
           |<span></span>
           |""".stripMargin)

    val accountsSection = HtmxCollapsible.section(
      title = "Accounts",
      id    = s"$formId-sec-accounts",
      content =
        s"""
           |${HtmxFields.accountRow("Stock account",   s"$formId-account", "account",        accounts, a.account)}
           |${HtmxFields.accountRow("Expense account", s"$formId-oaccount","oaccount",       accounts, a.oaccount)}
           |${HtmxFields.accountRow("Revenue account", s"$formId-revenue", "revenueAccount", accounts, a.revenueAccount)}
           |${HtmxFields.vatRow("VAT code",            s"$formId-vatCode", "vatCode",        vats,     a.vatCode)}
           |""".stripMargin)

    val stocksSection = HtmxCollapsible.section(
      title = "Stocks",
      id    = s"$formId-sec-stocks",
      content =
        s"""
           |<div class="subgrid-block">
           |  $stockGrid
           |</div>
           |""".stripMargin)

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/article"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", a.id)}
       |  ${HtmxFields.hiddenField("modelid", a.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", a.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  $summarySection
       |  $pricingSection
       |  $unitsSection
       |  $accountsSection
       |  $stocksSection
       |</form>
       |""".stripMargin