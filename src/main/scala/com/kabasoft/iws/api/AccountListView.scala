package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Account

object AccountListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("120px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("description", "Description"),
    HtmxTable.Column("currency",    "Currency",    Some("80px"))
  )

  def render(
              page:     HtmxTable.Page[Account],
              company:  String,
              query:    String,
              rowMode:  HtmxTable.RowMode = HtmxTable.RowMode.Navigate,
              detailFn: Account => String = a => s"/account-htmx/${a.id}/${a.modelid}/${a.company}"
            ): String =
    HtmxTable.render(
      path      = s"/html/account-list/$company",
      columns   = columns,
      p         = page,
      rowCells  = a => List(a.id, a.name, a.description, a.currency),
      detailUrl = detailFn,
      query     = query,
      rowMode   = rowMode
    )