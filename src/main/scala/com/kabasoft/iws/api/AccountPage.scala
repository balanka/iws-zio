package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, ModelId}
import com.kabasoft.iws.repository.AccountRepository
import zio._
import zio.http._

object AccountPage:

  private val pageGet: Route[AccountRepository, Response] =
    Method.GET / "account-htmx" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        HtmxEndpoint.loadPage(
          "Account", id, modelid, company,
          load   = (id, modelid, company) => AccountRepository.getById((id, modelid, company)),
          render = (acc: Account) =>
            AccountRepository.all((ModelId.ACCOUNT.modelid, acc.company))
              .map(all => AccountFormView.render(acc, all))
        )
      }

  val routes: Routes[AccountRepository, Response] = Routes(pageGet)