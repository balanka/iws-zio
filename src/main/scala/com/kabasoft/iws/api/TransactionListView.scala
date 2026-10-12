package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Transaction

object TransactionListView:

  private val columns = List(
    HtmxTable.Column("id",      "Id",      Some("100px")),
    HtmxTable.Column("oid",     "Oid"),
    HtmxTable.Column("modelid", "Model",   Some("80px")),
    HtmxTable.Column("period",  "Period",  Some("90px")),
    HtmxTable.Column("contact", "Contact"),
    HtmxTable.Column("account", "Account", Some("100px")),
    HtmxTable.Column("store",   "Store",   Some("100px")),
    HtmxTable.Column("text",    "Text"),
    HtmxTable.Column("total",   "Total",   Some("140px")),
    HtmxTable.Column("posted",  "Posted",  Some("80px"),
      kind = HtmxTable.ColumnKind.Bool)
  )

  private def total(t: Transaction): String =
    val sum = t.lines
      .map(_.amount)
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))
    val currencyCode =
      t.lines.headOption.map(_.currency).filter(_.nonEmpty).getOrElse("EUR")
    MoneyFmt.format(sum, currencyCode)

  def render(
              page:    HtmxTable.Page[Transaction],
              company: String,
              query:   String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#transaction-form")
            ): String =
    HtmxTable.render(
      path      = "/html/transaction-list",
      columns   = columns,
      p         = page,
      rowCells  = (t: Transaction) => List(
        t.id.toString,
        t.oid,
        t.modelid.toString,
        t.period.toString,
        t.contact,
        t.account,
        t.store,
        t.text,
        total(t),
        t.posted.toString
      ),
      cellClasses = (_: Transaction) => List(
        "", "", "", "", "", "", "", "",
        "num-cell",       // ← Total column right-aligned
        ""
      ),
      detailUrl = (t: Transaction) =>
        s"/html/transaction/${t.id}/${t.modelid}/${t.company}",
      query     = query,
      rowMode   = rowMode
    )