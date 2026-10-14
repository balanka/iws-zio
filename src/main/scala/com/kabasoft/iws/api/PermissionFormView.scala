package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Permission

object PermissionFormView:

  def render(p: Permission, mode: String = "view"): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "permission-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/permission"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", p.id.toString)}
       |  ${HtmxFields.hiddenField("modelid", p.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", p.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", p.id.toString, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", p.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", p.description)}
       |  ${HtmxFields.choiceRow("Right", s"$formId-short", "short", HtmxFields.permissionRights, p.short)}
       |
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin