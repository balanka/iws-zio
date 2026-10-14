package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Store

object StoreListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("100px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("description", "Description"),
    HtmxTable.Column("costcenter",  "CostCenter",  Some("110px"))
  )

  def render(
              page: HtmxTable.Page[Store], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#store-form"),
              detailFn: Store => String = s => s"/html/store/${s.id}/${s.modelid}/${s.company}"
            ): String =
    HtmxTable.render(
      path = s"/html/store-list/$company",
      columns = columns, p = page,
      rowCells = s => List(s.id, s.name, s.description, s.costcenter),
      detailUrl = detailFn, query = query, rowMode = rowMode)