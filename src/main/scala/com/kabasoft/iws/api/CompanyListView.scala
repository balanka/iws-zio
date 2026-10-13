package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Company

object CompanyListView:

  private val columns = List(
    HtmxTable.Column("id",    "Id",    Some("100px"), resizable = true),
    HtmxTable.Column("name",  "Name", Some("50px"), resizable = true),
    HtmxTable.Column("city",  "City", Some("50px"), resizable = true),
    HtmxTable.Column("country", "Country", Some("50px"), resizable = true),
    HtmxTable.Column("phone", "Phone", Some("50px"), resizable = true),
    HtmxTable.Column("email", "Email", Some("50px"), resizable = true),
    HtmxTable.Column("account", "Account", Some("50px"), resizable = true),
    HtmxTable.Column("oaccount", "OAccount", Some("50px"), resizable = true),
    HtmxTable.Column("vat", "Vat", Some("50px"), resizable = true),
  )

  def render(
              page: HtmxTable.Page[Company], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#company-form"),
              detailFn: Company => String = c => s"/html/company/${c.id}/${c.modelid}/${c.id}"
            ): String =
    HtmxTable.render(
      path      = s"/html/company-list/$company",
      columns   = columns,
      p         = page,
      rowCells  = c => List(c.id, c.name, c.city, c.country, c.email),
      detailUrl = detailFn,
      query     = query,
      rowMode   = rowMode)