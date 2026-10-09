package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Masterfile, Stock, Store}

object StoreFormView:

  private val stockColumns = List(
    HtmxSubGrid.Column("id",       "Id",       "w-24"),
    HtmxSubGrid.Column("article",  "Article",  "w-28"),
    HtmxSubGrid.Column("quantity", "Qty",      "num w-20"),
    HtmxSubGrid.Column("price",    "Price",    "num w-24"),
    HtmxSubGrid.Column("charge",   "Charge",   "w-20")
  )

  private def stockRows(s: Store): List[HtmxSubGrid.Row] =
    s.stocks.map(st => HtmxSubGrid.Row(
      cells = List(
        st.id,
        st.article,
        st.quantity.toString,
        st.price.toString,
        st.charge)))

  def render(
              s:           Store,
              accounts:    List[Account],
              costCenters: List[Masterfile],
              mode:        String = "view"
            ): String =
    val isCreate  = mode == "create"
    val formClass = if mode == "view" then "htmx-grid form-readonly" else "htmx-grid"
    val formId    = "store-form"

    val stockGrid = HtmxSubGrid.render(
      id       = s"$formId-stocks",
      prefix   = "stocks",
      fkField  = "store",
      fkValue  = s.id,
      columns  = stockColumns,
      rows     = stockRows(s))

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
       |  <div class="subgrid-block">
       |    <label class="label">Stocks</label>
       |    $stockGrid
       |  </div>
       |</form>
       |""".stripMargin