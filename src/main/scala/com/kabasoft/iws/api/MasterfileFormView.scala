package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Masterfile

object MasterfileFormView:

  def render(m: Masterfile, mode: String = "view"): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "masterfile-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/masterfile"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", m.id)}
       |  ${HtmxFields.hiddenField("modelid", m.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", m.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", m.id, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", m.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", m.description)}
       |  ${HtmxFields.textRow("Parent", s"$formId-parent", "parent", m.parent)}
       |
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin