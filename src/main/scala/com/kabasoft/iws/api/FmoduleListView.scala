package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Fmodule

object FmoduleListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("100px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("parent",      "Parent",      Some("100px")),
    HtmxTable.Column("description", "Description")
  )

  def render(
              page: HtmxTable.Page[Fmodule], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#fmodule-form"),
              detailFn: Fmodule => String = m => s"/html/fmodule/${m.id}/${m.modelid}/${m.company}"
            ): String =
    HtmxTable.render(
      path      = s"/html/fmodule-list/$company",
      columns   = columns,
      p         = page,
      rowCells  = m => List(m.id.toString, m.name, m.parent, m.description),
      detailUrl = detailFn,
      query     = query,
      rowMode   = rowMode
    )