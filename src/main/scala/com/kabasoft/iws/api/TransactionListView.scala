package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Transaction

object TransactionListView:

  private val columns = List(
    HtmxTable.Column("id",      "Id",      Some("24px")),
    HtmxTable.Column("oid",     "Oid", Some("24px"), resizable = true),
    HtmxTable.Column("modelid", "Model",   Some("18px")),
    HtmxTable.Column("period",  "Period",  Some("12px")),
    HtmxTable.Column("contact", "Contact", Some("40px"), resizable = true),
    HtmxTable.Column("account", "Account", Some("50px"), resizable = true),
    HtmxTable.Column("store",   "Store",   Some("50px"), resizable = true),
    HtmxTable.Column("text",    "Text",    Some("260px"), resizable = true),
    HtmxTable.Column("total",   "Total",   Some("40px"), resizable = true),
    HtmxTable.Column("posted",  "Posted",  Some("10px"), kind = HtmxTable.ColumnKind.Bool)
  )

  private def total(t: Transaction): String =
    val sum = t.lines
      .map(_.amount)
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))
    val currencyCode =
      t.lines.headOption.map(_.currency).filter(_.nonEmpty).getOrElse("EUR")
    MoneyFmt.format(sum, currencyCode)

  def render(page:    HtmxTable.Page[Transaction],
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
        "num-cell",
        ""
      ),
      detailUrl = (t: Transaction) =>
        s"/html/transaction/${t.id}/${t.modelid}/${t.company}",
      query     = query,
      rowMode   = rowMode
    )