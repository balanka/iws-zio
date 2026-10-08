package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, BusinessPartner, Contact, Employee, Vat}
import Html.esc

object BusinessPartnerFormView:

  def render(
              p:        BusinessPartner,
              accounts: List[Account],
              vats:     List[Vat],
              contacts: List[Contact],
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

    s"""
       |<form id="$formId"
       |      class="$formClass"
       |      hx-put="/html/$prefix"
       |      hx-target="#$formId"
       |      hx-swap="outerHTML">
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
       |</form>
       |""".stripMargin