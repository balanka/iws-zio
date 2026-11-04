package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, Transaction}
import zio._
import zio.http._
import java.time.{Instant, LocalDate}

object TransactionCombinedPage:

  type Repos = FormLists.WithArticlesAndVats & FormLists.Cache

  private def blank(company: String, modelid: Int): Transaction =
    val ld = LocalDate.now()
    Transaction(
      id = -1L, oid = "", contact = "", store = "", account = "-1",
      transdate = Instant.now(), enterdate = Instant.now(), postingdate = Instant.now(),
      period = ld.getYear * 100 + ld.getMonthValue,
      posted = false, modelid = modelid,
      company = company, text = "", footText = "", lines = Nil)

  private def renderForm(company: String, data: FormLists.FormData, initialModelid: Int): String =
    TransactionFormView.render(
      t                  = blank(company, initialModelid),
      fmodules           = data.fmodules,
      stores             = data.stores,
      accounts           = data.accounts,
      articles           = data.articles,
      vats               = data.vats,
      contacts           = data.contacts,
      copyFromCandidates = Nil,
      mode               = "view")

  val routes: Routes[Repos, Response] =
    CombinedPageSupport.routesWithArticlesAndVats(
      fullPathSegment     = "transaction-combined-htmx",
      fragmentPathSegment = "transaction-combined",
      parentModelId       = ModelId.TRANSACTION.modelid,
      title               = "Transaction",
      formId              = "transaction-form",
      newFormUrl          = "/html/transaction/new-form",
      newFormInclude      = "#transaction-form [name='modelid'], #transaction-form [name='company']",
      listPlaceholderId   = "transaction-list-placeholder",
      listPlaceholderText = "Select a transaction type above to see the list.",
      renderForm          = renderForm
    )