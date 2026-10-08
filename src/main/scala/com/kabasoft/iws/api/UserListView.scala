package com.kabasoft.iws.api

import com.kabasoft.iws.domain.User

object UserListView:

  private val columns = List(
    HtmxTable.Column("id",        "Id",         Some("80px")),
    HtmxTable.Column("userName",  "User name"),
    HtmxTable.Column("firstName", "First"),
    HtmxTable.Column("lastName",  "Last"),
    HtmxTable.Column("email",     "Email")
  )

  def render(
              page: HtmxTable.Page[User], company: String, query: String,
              rowMode: HtmxTable.RowMode = HtmxTable.RowMode.Fetch("#user-form"),
              detailFn: User => String = u => s"/html/user/${u.id}/${u.modelid}/${u.company}"
            ): String =
    HtmxTable.render(
      path      = s"/html/user-list/$company",
      columns   = columns,
      p         = page,
      rowCells  = u => List(u.id.toString, u.userName, u.firstName, u.lastName, u.email),
      detailUrl = detailFn,
      query     = query,
      rowMode   = rowMode
    )