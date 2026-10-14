package com.kabasoft.iws.api

import com.kabasoft.iws.domain.ModelId
import com.kabasoft.iws.repository.AccountRepository
import zio._
import zio.http._

object AccountListPage:

  private val pageGet: Route[AccountRepository, Response] =
    Method.GET / "account-list-htmx" / string("company") ->
      handler { (company: String, _: Request) =>
        AccountRepository
          .all((ModelId.ACCOUNT.modelid, company))
          .map { all =>
            val sorted = all.sortBy(_.id)
            val slice  = sorted.take(50)
            val table = AccountListView.render(
              HtmxTable.Page(slice, 0, 50, sorted.size.toLong),
              company,
              ""
            )
            HtmxResponse.html(
              HtmxPage.render("Accounts", table)
            )
          }
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[AccountRepository, Response] = Routes(pageGet)