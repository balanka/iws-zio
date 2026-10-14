package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Transaction

object TransactionListView:

  private val columns = List(
    HtmxTable.Column("id",      "Id",      Some("100px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("oid",     "Oid",
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("modelid", "Model",   Some("80px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("period",  "Period",  Some("90px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("contact", "Contact",
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("account", "Account", Some("100px"),
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("store",   "Store",   Some("100px"),
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("text",    "Text",    Some("260px"),
      filter    = HtmxTable.FilterKind.Text,
      resizable = true),
    HtmxTable.Column("total",   "Total",   Some("140px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("posted",  "Posted",  Some("80px"),
      filter        = HtmxTable.FilterKind.Select,
      filterOptions = List("true" -> "Posted", "false" -> "Draft"),
      kind          = HtmxTable.ColumnKind.Bool)
  )

  private def total(t: Transaction): String =
    val sum = t.lines
      .map(_.amount)
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))
    val currencyCode =
      t.lines.headOption.map(_.currency).filter(_.nonEmpty).getOrElse("EUR")
    MoneyFmt.format(sum, currencyCode)

  def render(
              page:         HtmxTable.Page[Transaction],
              company:      String,
              query:        String,
              filterValues: Map[String, String] = Map.empty,
              rowMode:      HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#transaction-form")
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
      query        = query,
      rowMode      = rowMode,
      filterValues = filterValues
    )