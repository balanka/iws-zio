package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Transaction

object TransactionListView:

  private val columns = List(
    HtmxTable.Column("id",      "Id",      Some("100px")),
    HtmxTable.Column("oid",     "Oid"),
    HtmxTable.Column("period",  "Period"),
    HtmxTable.Column("contact", "Contact"),
    HtmxTable.Column("text",    "Text"),
    HtmxTable.Column("posted",  "Posted")
  )

  def render(
              page:    HtmxTable.Page[Transaction],
              company: String,
              query:   String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#transaction-form")
            ): String =
    HtmxTable.render(
      path      = "/html/transaction-list",       // ← no company segment
      columns   = columns,
      p         = page,
      rowCells  = (t: Transaction) => List(
        t.id.toString,
        t.oid,
        t.period.toString,
        t.contact,
        t.text,
        t.posted.toString),
      detailUrl = (t: Transaction) => s"/html/transaction/${t.id}/${t.modelid}/${t.company}",
      query     = query,
      rowMode   = rowMode
    )