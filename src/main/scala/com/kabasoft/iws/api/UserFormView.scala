package com.kabasoft.iws.api

import com.kabasoft.iws.domain.User

object UserFormView:

  def render(u: User, mode: String = "view"): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "user-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/user"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", u.id.toString)}
       |  ${HtmxFields.hiddenField("modelid", u.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", u.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", u.id.toString, readonly = !isCreate)}
       |  ${HtmxFields.textRow("User name", s"$formId-uname", "userName", u.userName)}
       |
       |  ${HtmxFields.textRow("First name", s"$formId-fname", "firstName", u.firstName)}
       |  ${HtmxFields.textRow("Last name", s"$formId-lname", "lastName", u.lastName)}
       |
       |  ${HtmxFields.textRow("Email", s"$formId-email", "email", u.email)}
       |  ${HtmxFields.textRow("Phone", s"$formId-phone", "phone", u.phone)}
       |
       |  ${HtmxFields.textRow("Department", s"$formId-dept", "department", u.department)}
       |  ${HtmxFields.textRow("Menu", s"$formId-menu", "menu", u.menu)}
       |
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin