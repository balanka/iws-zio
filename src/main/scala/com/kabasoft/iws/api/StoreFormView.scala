package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Masterfile, Store}

object StoreFormView:

  def render(
              s:           Store,
              accounts:    List[Account],
              costCenters: List[Masterfile],
              mode:        String = "view"
            ): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "store-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/store"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", s.id)}
       |  ${HtmxFields.hiddenField("modelid", s.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", s.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", s.id, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", s.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", s.description)}
       |  ${HtmxFields.masterfileRow("Cost center", s"$formId-cc", "costcenter", costCenters, s.costcenter)}
       |
       |  ${HtmxFields.accountRow("Stock account", s"$formId-account", "account", accounts, s.account)}
       |  ${HtmxFields.accountRow("Expense account", s"$formId-oaccount", "oaccount", accounts, s.oaccount)}
       |
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin