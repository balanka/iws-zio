package com.kabasoft.iws.api

import com.kabasoft.iws.domain.FinancialsTransaction

object FinancialsListView:

  private val columns = List(
    HtmxTable.Column("id",         "Id",          Some("40px"),
      filter = HtmxTable.FilterKind.Number, resizable = true),
    HtmxTable.Column("oid",        "Oid", Some("40px"),
      filter = HtmxTable.FilterKind.Text, resizable = true),
    HtmxTable.Column("modelid",    "Model",       Some("20px"),
      filter = HtmxTable.FilterKind.Number, resizable = true),
    HtmxTable.Column("period",     "Period",  Some("16px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("contact",    "Contact", Some("20px"),
      filter = HtmxTable.FilterKind.Text,  resizable = true),
    HtmxTable.Column("costcenter", "Cost center", Some("50px"),
      filter = HtmxTable.FilterKind.Text, resizable = true),
    HtmxTable.Column("text",       "Text",        Some("260px"),
      filter    = HtmxTable.FilterKind.Text, resizable = true),
    HtmxTable.Column("total",      "Total",       Some("140px"),
      filter = HtmxTable.FilterKind.Number, resizable = true),
    HtmxTable.Column("posted",     "Posted",      Some("14px"),
      filter        = HtmxTable.FilterKind.Select,
      filterOptions = List("true" -> "Posted", "false" -> "Draft"),
      kind          = HtmxTable.ColumnKind.Bool)
  )

  private def total(t: FinancialsTransaction): String =
    val sum = t.lines
      .map(_.amount)
      .foldLeft(java.math.BigDecimal.ZERO)(_.add(_))
    val currencyCode =
      t.lines.headOption.map(_.currency).filter(_.nonEmpty).getOrElse("EUR")
    MoneyFmt.format(sum, currencyCode)

  def render(
              page:         HtmxTable.Page[FinancialsTransaction],
              company:      String,
              query:        String,
              filterValues: Map[String, String] = Map.empty,
              rowMode:      HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#financials-form")
            ): String =
    HtmxTable.render(
      path         = "/html/financials-list",
      columns      = columns,
      p            = page,
      rowCells     = (t: FinancialsTransaction) => List(
        t.id.toString,
        t.oid,
        t.modelid.toString,
        t.period.toString,
        t.contact,
        t.costcenter,
        t.text,
        total(t),
        t.posted.toString
      ),
      cellClasses  = (_: FinancialsTransaction) => List(
        "", "", "", "", "", "", "",
        "num-cell",       // ← Total — right-aligned
        ""
      ),
      detailUrl    = (t: FinancialsTransaction) =>
        s"/html/financials/${t.id}/${t.modelid}/${t.company}",
      query        = query,
      rowMode      = rowMode,
      filterValues = filterValues
    )