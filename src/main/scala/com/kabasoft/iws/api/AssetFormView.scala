package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Asset}

object AssetFormView:

  def render(a: Asset, accounts: List[Account], mode: String = "view"): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "asset-form"

    s"""
       |<form id="$formId" class="$formClass"
       |      hx-put="/html/asset"
       |      hx-target="#$formId" hx-swap="outerHTML">
       |
       |  ${HtmxFields.hiddenField("id", a.id)}
       |  ${HtmxFields.hiddenField("modelid", a.modelid.toString)}
       |  ${HtmxFields.hiddenField("company", a.company)}
       |  ${HtmxFields.hiddenField("mode", mode)}
       |
       |  ${HtmxFields.textRow("Id", s"$formId-id", "id_display", a.id, readonly = !isCreate)}
       |  ${HtmxFields.textRow("Name", s"$formId-name", "name", a.name)}
       |
       |  ${HtmxFields.textareaRow("Description", s"$formId-desc", "description", a.description)}
       |  ${HtmxFields.currencyRow("Currency", s"$formId-ccy", "currency", a.currency)}
       |
       |  ${HtmxFields.moneyRow("Amount", s"$formId-amount", "amount", a.amount.toString, a.currency)}
       |  ${HtmxFields.moneyRow("Scrap value", s"$formId-scrap", "scrapValue", a.scrapValue.toString, a.currency)}
       |
       |  ${HtmxFields.numberRow("Life span (months)", s"$formId-life", "lifeSpan", a.lifeSpan.toString)}
       |  ${HtmxFields.numberRow("Frequency", s"$formId-freq", "frequency", a.frequency.toString)}
       |
       |  ${HtmxFields.numberRow("Rate", s"$formId-rate", "rate", a.rate.toString)}
       |  ${HtmxFields.numberRow("Dep. method", s"$formId-dep", "depMethod", a.depMethod.toString)}
       |
       |  ${HtmxFields.accountRow("Account", s"$formId-account", "account", accounts, a.account)}
       |  ${HtmxFields.accountRow("Oaccount", s"$formId-oaccount", "oaccount", accounts, a.oaccount)}
       |
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin