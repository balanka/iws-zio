package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Asset

object AssetListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",  Some("50px"), resizable = true),
    HtmxTable.Column("name",        "Name", Some("100px"), resizable = true),
    HtmxTable.Column("description", "Description", Some("200px"), resizable = true),
    HtmxTable.Column("amount",      "Amount",      Some("50px"), resizable = true)
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