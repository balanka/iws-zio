package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Vat}
import Html.esc

object VatFormView:

  def render(
              v:        Vat,
              accounts: List[Account],
              mode:     String = "view"
            ): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "vat-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/vat"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", v.id)}
       |  ${HtmxFields.hiddenField("modelid", v.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", v.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", v.id, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", v.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", v.description)}
       |  ${HtmxFields.numberRow("Percent", s"$formId-percent", "percent", v.percent.toString)}
       |
       |  ${HtmxFields.accountRow("Input VAT account", s"$formId-input", "inputVatAccount", accounts, v.inputVatAccount)}
       |  ${HtmxFields.accountRow("Output VAT account", s"$formId-output", "outputVatAccount", accounts, v.outputVatAccount)}
       |
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin