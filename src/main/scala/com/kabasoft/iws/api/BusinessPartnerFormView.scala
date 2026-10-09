package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, BusinessPartner, Contact, Employee, Masterfile, Vat}

object BusinessPartnerFormView:

  private def bankColumns(banks: List[Masterfile]): List[HtmxSubGrid.Column] =
    val bicOptions = banks.map(m => m.id -> s"${m.id} ${m.name}")
    List(
      HtmxSubGrid.Column("id",    "Id",    "w-24"),
      HtmxSubGrid.Column("bic",   "BIC",   "w-40", options = bicOptions),
      HtmxSubGrid.Column("owner", "Owner", "w-40"))

  private def bankRows(p: BusinessPartner): List[HtmxSubGrid.Row] =
    p.bankaccounts.map(b => HtmxSubGrid.Row(cells = List(b.id, b.bic, b.owner)))

  def render(
              p:        BusinessPartner,
              accounts: List[Account],
              vats:     List[Vat],
              contacts: List[Contact],
              banks:    List[Masterfile],
              kind:     PartnerKind,
              mode:     String = "view"
            ): String =
    val prefix    = kind.urlPrefix
    val formId    = s"$prefix-form"
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"

    val salaryRow =
      if kind.hasSalary then
        val salary = p match
          case e: Employee => e.salary.toString
          case _           => "0.00"
        s"${HtmxFields.moneyRow("Salary", s"$formId-salary", "salary", salary, p.currency)}\n<span></span>\n<span></span>"
      else ""

    val bankGrid = HtmxSubGrid.render(
      id       = s"$formId-banks",
      prefix   = "bankaccounts",
      fkField  = "owner",
      fkValue  = p.id,
      columns  = bankColumns(banks),
      rows     = bankRows(p))

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/$prefix"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", p.id)}
       |  ${HtmxFields.hiddenField("modelid", p.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", p.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", p.id, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", p.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", p.description)}
       |  ${HtmxFields.contactRow("Contact", s"$formId-contact", "contact", contacts, p.contact)}
       |
       |  ${HtmxFields.textRow("Street", s"$formId-street", "street", p.street)}
       |  ${HtmxFields.textRow("Zip", s"$formId-zip", "zip", p.zip)}
       |
       |  ${HtmxFields.textRow("City", s"$formId-city", "city", p.city)}
       |  ${HtmxFields.textRow("State", s"$formId-state", "state", p.state)}
       |
       |  ${HtmxFields.textRow("Country", s"$formId-country", "country", p.country)}
       |  ${HtmxFields.textRow("Phone", s"$formId-phone", "phone", p.phone)}
       |
       |  ${HtmxFields.textRow("Email", s"$formId-email", "email", p.email)}
       |  ${HtmxFields.currencyRow("Currency", s"$formId-currency", "currency", p.currency)}
       |
       |  ${HtmxFields.accountRow("Account", s"$formId-account", "account", accounts, p.account)}
       |  ${HtmxFields.accountRow("Oaccount", s"$formId-oaccount", "oaccount", accounts, p.oaccount)}
       |
       |  ${HtmxFields.textRow("Tax code", s"$formId-taxCode", "taxCode", p.taxCode)}
       |  ${HtmxFields.vatRow("VAT code", s"$formId-vatCode", "vatCode", vats, p.vatCode)}
       |$salaryRow
       |
       |  <div class="subgrid-block">
       |    <label class="label">Bank accounts</label>
       |    $bankGrid
       |  </div>
       |</form>
       |""".stripMargin