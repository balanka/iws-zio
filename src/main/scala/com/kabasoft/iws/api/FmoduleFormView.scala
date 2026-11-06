package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Fmodule}

object FmoduleFormView:

  def render(m: Fmodule, accounts: List[Account], mode: String = "view"): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "fmodule-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/fmodule"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", m.id.toString)}
       |  ${HtmxFields.hiddenField("modelid", m.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", m.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", m.id.toString, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", m.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", m.description)}
       |  ${HtmxFields.textRow("Parent", s"$formId-parent", "parent", m.parent)}
       |
       |  ${HtmxFields.textRow("Copy from", s"$formId-copy", "copyFrom", m.copyFrom)}
       |  ${HtmxFields.accountRow("Account", s"$formId-account", "account", accounts, m.account)}
       |
       |  ${HtmxFields.textRow("Account filter", s"$formId-accF", "accFilter", m.accFilter)}
       |  ${HtmxFields.textRow("Oaccount filter", s"$formId-oaccF", "oaccFilter", m.oaccFilter)}
       |
       |  ${HtmxFields.textRow("Template 1", s"$formId-tpl1", "template1", m.template1)}
       |  ${HtmxFields.textRow("Template 2", s"$formId-tpl2", "template2", m.template2)}
       |
       |  ${HtmxFields.checkboxRow("Is debit", s"$formId-isDebit", "isDebit", m.isDebit)}
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin