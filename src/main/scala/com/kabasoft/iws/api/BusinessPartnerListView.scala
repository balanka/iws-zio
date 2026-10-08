package com.kabasoft.iws.api

import com.kabasoft.iws.domain.BusinessPartner

object BusinessPartnerListView:

  private val columns = List(
    HtmxTable.Column("id",    "Id",    Some("100px")),
    HtmxTable.Column("name",  "Name"),
    HtmxTable.Column("city",  "City"),
    HtmxTable.Column("phone", "Phone"),
    HtmxTable.Column("email", "Email")
  )

  def render(
              page:    HtmxTable.Page[BusinessPartner],
              company: String,
              query:   String,
              kind:    PartnerKind,
              rowMode: HtmxTable.RowMode = null,   // ignored — set below from kind
              detailFn: BusinessPartner => String = null
            ): String =
    val prefix = kind.urlPrefix
    val mode = Option(rowMode).getOrElse(
      HtmxTable.RowMode.Fetch(s"#$prefix-form")
    )
    val detail = Option(detailFn).getOrElse(
      (p: BusinessPartner) => s"/html/$prefix/${p.id}/${p.modelid}/${p.company}"
    )

    HtmxTable.render(
      path      = s"/html/$prefix-list/$company",
      columns   = columns,
      p         = page,
      rowCells  = p => List(p.id, p.name, p.city, p.phone, p.email),
      detailUrl = detail,
      query     = query,
      rowMode   = mode
    )