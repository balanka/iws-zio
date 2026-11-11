package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Contact

object ContactFormView:

  def render(c: Contact, mode: String = "view"): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "contact-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/contact"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", c.id)}
       |  ${HtmxFields.hiddenField("modelid", c.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", c.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", c.id, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", c.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", c.description)}
       |  ${HtmxFields.textRow("Street", s"$formId-street", "street", c.street)}
       |
       |  ${HtmxFields.textRow("Zip", s"$formId-zip", "zip", c.zip)}
       |  ${HtmxFields.textRow("City", s"$formId-city", "city", c.city)}
       |
       |  ${HtmxFields.textRow("State", s"$formId-state", "state", c.state)}
       |  ${HtmxFields.textRow("Country", s"$formId-country", "country", c.country)}
       |
       |  ${HtmxFields.textRow("Phone", s"$formId-phone", "phone", c.phone)}
       |  ${HtmxFields.textRow("Email", s"$formId-email", "email", c.email)}
       |
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin