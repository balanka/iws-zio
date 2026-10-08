package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Vat

object VatListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("100px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("description", "Description"),
    HtmxTable.Column("percent",     "Percent",     Some("90px"))
  )

  def render(
              page: HtmxTable.Page[Vat], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#vat-form"),
              detailFn: Vat => String = v => s"/html/vat/${v.id}/${v.modelid}/${v.company}"
            ): String =
    HtmxTable.render(
      path        = s"/html/vat-list/$company",
      columns     = columns,
      p           = page,
      rowCells    = v => List(v.id, v.name, v.description, v.percent.toString),
      cellClasses = _ => List("", "", "", "num-cell"),
      detailUrl   = detailFn,
      query       = query,
      rowMode     = rowMode
    )