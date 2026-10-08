package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Article

object ArticleListView:

  private val columns = List(
    HtmxTable.Column("id",          "Id",          Some("100px")),
    HtmxTable.Column("name",        "Name"),
    HtmxTable.Column("description", "Description"),
    HtmxTable.Column("sprice",      "Sales price", Some("130px"))
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