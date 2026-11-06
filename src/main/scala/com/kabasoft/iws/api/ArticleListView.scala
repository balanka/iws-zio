package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Article

object ArticleListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("20px"), resizable = true),
    HtmxTable.Column("name",        "Name", Some("20px"), resizable = true),
    HtmxTable.Column("description", "Description", Some("200px"), resizable = true),
    HtmxTable.Column("pprice",      "Purchase price", Some("40px"), resizable = true),
    HtmxTable.Column("sprice",      "Sales price", Some("40px"), resizable = true),
    HtmxTable.Column("avgPrice",      "Av. price", Some("40px"), resizable = true)
  )

  def render(
              page:     HtmxTable.Page[Article],
              company:  String,
              query:    String,
              rowMode:  HtmxTable.RowMode = HtmxTable.RowMode.Navigate,
              detailFn: Article => String = a => s"/article-htmx/${a.id}/${a.modelid}/${a.company}"
            ): String =
    HtmxTable.render(
      path        = s"/html/article-list/$company",
      columns     = columns,
      p           = page,
      rowCells    = a => List(
        a.id,
        a.name,
        a.description,
        Format.money(a.sprice, a.currency)
      ),
      cellClasses = _ => List("", "", "", "num-cell"),
      detailUrl   = detailFn,
      query       = query,
      rowMode     = rowMode
    )