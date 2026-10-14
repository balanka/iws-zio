package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Article, ModelId}
import com.kabasoft.iws.repository.{AccountRepository, ArticleRepository, VatRepository}
import zio._
import zio.http._

object ArticlePage:

  private val pageGet: Route[ArticleRepository & AccountRepository & VatRepository, Response] =
    Method.GET / "article-htmx" / string("id") / int("modelid") / string("company") ->
      handler { (id: String, modelid: Int, company: String, _: Request) =>
        HtmxEndpoint.loadPage(
          "Article", id, modelid, company,
          load = (id, modelid, company) => ArticleRepository.getById((id, modelid, company)),
          render = (a: Article) =>
            for {
              accs <- AccountRepository.all((ModelId.ACCOUNT.modelid, a.company))
              vats <- VatRepository.all((ModelId.VAT.modelid, a.company))
            } yield ArticleFormView.render(a, accs, vats, mode = "view")
        )
      }

  val routes: Routes[ArticleRepository & AccountRepository & VatRepository, Response] =
    Routes(pageGet)