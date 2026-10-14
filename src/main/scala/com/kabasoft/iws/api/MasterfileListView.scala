package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Masterfile

object MasterfileListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("100px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("description", "Description"),
    HtmxTable.Column("parent",      "Parent",      Some("100px"))
  )

  def render(
              page:     HtmxTable.Page[Masterfile],
              modelid:  Int,
              company:  String,
              query:    String
            ): String =
    HtmxTable.render(
      path      = s"/html/masterfile-list/$modelid/$company",
      columns   = columns,
      p         = page,
      rowCells  = m => List(m.id, m.name, m.description, m.parent),
      detailUrl = m => s"/html/masterfile/${m.id}/${m.modelid}/${m.company}",
      query     = query,
      rowMode   = HtmxTable.RowMode.Fetch("#masterfile-form"))