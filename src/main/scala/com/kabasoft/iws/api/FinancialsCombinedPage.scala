package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{FinancialsTransaction, ModelId}
import zio._
import zio.http._
import java.time.{Instant, LocalDate}

object FinancialsCombinedPage:

  type Repos = FormLists.BaseEnv & FormLists.Cache

  private def blank(company: String, modelid: Int): FinancialsTransaction =
    val ld = LocalDate.now()
    FinancialsTransaction(
      id = -1L, oid = "", contact = "", costcenter = "", account = "-1",
      transdate = Instant.now(), enterdate = Instant.now(), postingdate = Instant.now(),
      period = ld.getYear * 100 + ld.getMonthValue,
      posted = false, modelid = modelid,
      company = company, text = "", footText = "", lines = Nil)

  private def renderForm(company: String, data: FormLists.FormData, initialModelid: Int): String =
    FinancialsFormView.render(
      t                  = blank(company, initialModelid),
      fmodules           = data.fmodules,
      stores             = data.stores,
      accounts           = data.accounts,
      contacts           = data.contacts,
      copyFromCandidates = Nil,
      mode               = "view")

  val routes: Routes[Repos, Response] =
    CombinedPageSupport.routesBase(
      fullPathSegment     = "financials-combined-htmx",
      fragmentPathSegment = "financials-combined",
      parentModelId       = ModelId.FINANCIALS.modelid,
      title               = "Financials",
      formId              = "financials-form",
      newFormUrl          = "/html/financials/new-form",
      newFormInclude      = "#financials-form [name='modelid'], #financials-form [name='company']",
      listPlaceholderId   = "financials-list-placeholder",
      listPlaceholderText = "Select a financials type above to see the list.",
      renderForm          = renderForm
    )