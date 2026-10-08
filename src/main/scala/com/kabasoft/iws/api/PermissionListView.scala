package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Permission

object PermissionListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("100px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("short",       "Short",       Some("90px")),
    HtmxTable.Column("description", "Description")
  )

  def render(
              page: HtmxTable.Page[Permission], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#permission-form"),
              detailFn: Permission => String = p => s"/html/permission/${p.id}/${p.modelid}/${p.company}"
            ): String =
    HtmxTable.render(
      path      = s"/html/permission-list/$company",
      columns   = columns,
      p         = page,
      rowCells  = p => List(p.id.toString, p.name, p.short, p.description),
      detailUrl = detailFn,
      query     = query,
      rowMode   = rowMode
    )