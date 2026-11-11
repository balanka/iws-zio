package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Company, Masterfile}

object CompanyFormView:

  private def bankColumns(banks: List[Masterfile]): List[HtmxSubGrid.Column] =
    val bicOptions = banks.map(m => m.id -> s"${m.id} ${m.name}")
    List(
      HtmxSubGrid.Column("id",    "Id",    "w-24"),
      HtmxSubGrid.Column("bic",   "BIC",   "w-40", options = bicOptions),
      HtmxSubGrid.Column("owner", "Owner", "w-40", readonly = true))

  private def bankRows(c: Company): List[HtmxSubGrid.Row] =
    c.bankaccounts.map(b => HtmxSubGrid.Row(cells = List(b.id, b.bic, b.owner)))

  def render(
              c:        Company,
              accounts: List[Account],
              banks:    List[Masterfile],
              mode:     String = "view"
            ): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-form form-readonly" else "htmx-form"
    val formId    = "company-form"

    val bankGrid = HtmxSubGrid.render(
      id       = s"$formId-banks",
      prefix   = "bankaccounts",
      fkField  = "owner",
      fkValue  = c.id,
      columns  = bankColumns(banks),
      rows     = bankRows(c))

    // ── Sections ────────────────────────────────────────────────────────

    val summarySection = HtmxCollapsible.section(
      title = "Summary",
      id    = s"$formId-sec-summary",
      open  = true,
      content =
        s"""
           |${HtmxFields.textRow("Id",   s"$formId-id",   "id_display", c.id, readonly = !isCreate)}
           |${HtmxFields.textRow("Name", s"$formId-name", "name", c.name)}
           |${HtmxFields.textareaRow("Description", s"$formId-desc", "description", c.description)}
           |${HtmxFields.textRow("Contact", s"$formId-contact", "contact", c.contact)}
           |""".stripMargin)

    val addressSection = HtmxCollapsible.section(
      title = "Address",
      id    = s"$formId-sec-address",
      content =
        s"""
           |${HtmxFields.textRow("Street",  s"$formId-street",  "street",  c.street)}
           |${HtmxFields.textRow("Zip",     s"$formId-zip",     "zip",     c.zip)}
           |${HtmxFields.textRow("City",    s"$formId-city",    "city",    c.city)}
           |${HtmxFields.textRow("State",   s"$formId-state",   "state",   c.state)}
           |${HtmxFields.textRow("Country", s"$formId-country", "country", c.country)}
           |""".stripMargin)

    val communicationSection = HtmxCollapsible.section(
      title = "Communication",
      id    = s"$formId-sec-comm",
      content =
        s"""
           |${HtmxFields.textRow("Phone", s"$formId-phone", "phone", c.phone)}
           |${HtmxFields.textRow("Email", s"$formId-email", "email", c.email)}
           |""".stripMargin)

    val localeTaxSection = HtmxCollapsible.section(
      title = "Locale & Tax",
      id    = s"$formId-sec-locale",
      content =
        s"""
           |${HtmxFields.currencyRow("Currency", s"$formId-currency", "currency", c.currency)}
           |${HtmxFields.textRow("Locale",        s"$formId-locale",  "locale",   c.locale)}
           |${HtmxFields.textRow("Tax code",      s"$formId-tax",     "taxCode",  c.taxCode)}
           |${HtmxFields.textRow("VAT code",      s"$formId-vat",     "vatCode",  c.vatCode)}
           |${HtmxFields.textRow("Bank account",  s"$formId-bankAcc", "bankAcc",  c.bankAcc)}
           |""".stripMargin)

    val chartSection = HtmxCollapsible.section(
      title = "Chart of Accounts",
      id    = s"$formId-sec-chart",
      content =
        s"""
           |${HtmxFields.accountRow("Account",           s"$formId-account",  "account",         accounts, c.account)}
           |${HtmxFields.accountRow("Oaccount",          s"$formId-oaccount", "oaccount",        accounts, c.oaccount)}
           |${HtmxFields.accountRow("Balance sheet acc", s"$formId-bsAcc",    "balanceSheetAcc", accounts, c.balanceSheetAcc)}
           |${HtmxFields.accountRow("Income stmt acc",   s"$formId-isAcc",    "incomeStmtAcc",   accounts, c.incomeStmtAcc)}
           |""".stripMargin)

    val clearingSection = HtmxCollapsible.section(
      title = "Clearing & Cash",
      id    = s"$formId-sec-clearing",
      content =
        s"""
           |${HtmxFields.accountRow("Purchasing clearing", s"$formId-pcAcc",   "purchasingClearingAcc", accounts, c.purchasingClearingAcc)}
           |${HtmxFields.accountRow("Sales clearing",      s"$formId-scAcc",   "salesClearingAcc",      accounts, c.salesClearingAcc)}
           |${HtmxFields.accountRow("Cash account",        s"$formId-cashAcc", "cashAcc",               accounts, c.cashAcc)}
           |""".stripMargin)

    val banksSection = HtmxCollapsible.section(
      title = "Bank accounts",
      id    = s"$formId-sec-banks",
      content =
        s"""
           |<div class="subgrid-block">
           |  $bankGrid
           |</div>
           |""".stripMargin)

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/company"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", c.id)}
       |  ${HtmxFields.hiddenField("modelid", c.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", c.id)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  $summarySection
       |  $addressSection
       |  $communicationSection
       |  $localeTaxSection
       |  $chartSection
       |  $clearingSection
       |  $banksSection
       |</form>
       |""".stripMargin