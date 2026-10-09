package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Asset

object AssetListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("100px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("description", "Description"),
    HtmxTable.Column("amount",      "Amount",      Some("120px"))
  )

  def render(
              page: HtmxTable.Page[Asset], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#asset-form"),
              detailFn: Asset => String = a => s"/html/asset/${a.id}/${a.modelid}/${a.company}"
            ): String =
    HtmxTable.render(
      path = s"/html/asset-list/$company",
      columns = columns, p = page,
      rowCells = a => List(a.id, a.name, a.description, a.amount.toString),
      cellClasses = _ => List("", "", "", "num-cell"),
      detailUrl = detailFn, query = query, rowMode = rowMode)