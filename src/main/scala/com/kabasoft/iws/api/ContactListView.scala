package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Contact

object ContactListView:

  private val columns = List(
    HtmxTable.Column("id",    "Id",    Some("40px"), resizable = true),
    HtmxTable.Column("name",  "Name",    Some("40px"), resizable = true),
    HtmxTable.Column("city",  "City",    Some("40px"), resizable = true),
    HtmxTable.Column("phone", "Phone",    Some("40px"), resizable = true),
    HtmxTable.Column("email", "Email",    Some("40px"), resizable = true)
  )

  def render(
              page: HtmxTable.Page[Contact], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#contact-form"),
              detailFn: Contact => String = c => s"/html/contact/${c.id}/${c.modelid}/${c.company}"
            ): String =
    HtmxTable.render(
      path = s"/html/contact-list/$company",
      columns = columns, p = page,
      rowCells = c => List(c.id, c.name, c.city, c.phone, c.email),
      detailUrl = detailFn, query = query, rowMode = rowMode)