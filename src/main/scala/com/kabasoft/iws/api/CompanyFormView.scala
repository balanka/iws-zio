package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, BankAccount, Company}
import Html.esc

object CompanyFormView:

  private val bankColumns = List(
    HtmxSubGrid.Column("id",    "Id",    "w-24"),
    HtmxSubGrid.Column("bic",   "BIC",   "w-28"),
    HtmxSubGrid.Column("owner", "Owner", "w-40")
  )

  private def bankRows(c: Company): List[HtmxSubGrid.Row] =
    c.bankaccounts.map(b => HtmxSubGrid.Row(
      cells = List(b.id, b.bic, b.owner)))

  def render(
              c:        Company,
              accounts: List[Account],
              mode:     String = "view"
            ): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "company-form"

    val bankGrid = HtmxSubGrid.render(
      id       = s"$formId-banks",
      prefix   = "bankaccounts",
      fkField  = "owner",
      fkValue  = c.id,
      columns  = bankColumns,
      rows     = bankRows(c))

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
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", c.id, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", c.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", c.description)}
       |  ${HtmxFields.textRow("Contact", s"$formId-contact", "contact", c.contact)}
       |
       |  ${HtmxFields.textRow("Street", s"$formId-street", "street", c.street)}
       |  ${HtmxFields.textRow("Zip", s"$formId-zip", "zip", c.zip)}
       |
       |  ${HtmxFields.textRow("City", s"$formId-city", "city", c.city)}
       |  ${HtmxFields.textRow("State", s"$formId-state", "state", c.state)}
       |
       |  ${HtmxFields.textRow("Country", s"$formId-country", "country", c.country)}
       |  ${HtmxFields.textRow("Phone", s"$formId-phone", "phone", c.phone)}
       |
       |  ${HtmxFields.textRow("Email", s"$formId-email", "email", c.email)}
       |  ${HtmxFields.currencyRow("Currency", s"$formId-currency", "currency", c.currency)}
       |
       |  ${HtmxFields.textRow("Locale", s"$formId-locale", "locale", c.locale)}
       |  ${HtmxFields.textRow("Tax code", s"$formId-tax", "taxCode", c.taxCode)}
       |
       |  ${HtmxFields.textRow("VAT code", s"$formId-vat", "vatCode", c.vatCode)}
       |  ${HtmxFields.textRow("Bank account", s"$formId-bankAcc", "bankAcc", c.bankAcc)}
       |
       |  ${HtmxFields.accountRow("Account", s"$formId-account", "account", accounts, c.account)}
       |  ${HtmxFields.accountRow("Oaccount", s"$formId-oaccount", "oaccount", accounts, c.oaccount)}
       |
       |  ${HtmxFields.accountRow("Balance sheet acc", s"$formId-bsAcc", "balanceSheetAcc", accounts, c.balanceSheetAcc)}
       |  ${HtmxFields.accountRow("Income stmt acc", s"$formId-isAcc", "incomeStmtAcc", accounts, c.incomeStmtAcc)}
       |
       |  ${HtmxFields.accountRow("Purchasing clearing", s"$formId-pcAcc", "purchasingClearingAcc", accounts, c.purchasingClearingAcc)}
       |  ${HtmxFields.accountRow("Sales clearing", s"$formId-scAcc", "salesClearingAcc", accounts, c.salesClearingAcc)}
       |
       |  ${HtmxFields.accountRow("Cash account", s"$formId-cashAcc", "cashAcc", accounts, c.cashAcc)}
       |  <span></span>
       |  <span></span>
       |
       |  <div class="subgrid-block">
       |    <label class="label">Bank accounts</label>
       |    $bankGrid
       |  </div>
       |</form>
       |""".stripMargin