package com.kabasoft.iws.api

import com.kabasoft.iws.api.HtmxSubGrid.CellKind.Money
import com.kabasoft.iws.domain.FinancialsTransaction

object FinancialsListView:

  private val columns = List(
    HtmxTable.Column("id",         "Id",          Some("100px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("oid",        "Oid",
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("modelid",    "Model",       Some("80px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("period",     "Period",
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("contact",    "Contact",
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("costcenter", "Cost center",
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("text",       "Text",
      filter = HtmxTable.FilterKind.Text),
    HtmxTable.Column("total",      "Total",       Some("140px"),
      filter = HtmxTable.FilterKind.Number),
    HtmxTable.Column("posted",     "Posted",      Some("80px"),
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
      detailUrl    = (t: FinancialsTransaction) =>
        s"/html/financials/${t.id}/${t.modelid}/${t.company}",
      query        = query,
      rowMode      = rowMode,
      filterValues = filterValues
    )