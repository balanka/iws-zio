package com.kabasoft.iws.api

import com.kabasoft.iws.domain.Account
import Html.esc

object AccountFormView:

  private def parentOptions(accounts: List[Account], selected: String, excludeId: String): String =
    val noneSel = if (selected == "-1" || selected.isEmpty) " selected" else ""
    val none = s"""<option class="opt-even" value="-1"$noneSel>— none —</option>"""
    val rest = accounts
      .filterNot(_.id == excludeId)
      .zipWithIndex
      .map { case (a, i) =>
        val sel = if (a.id == selected) " selected" else ""
        val cls = if (i % 2 == 0) "opt-odd" else "opt-even"
        s"""<option class="$cls" value="${esc(a.id)}"$sel>${esc(a.id + " " + a.name)}</option>"""
      }.mkString
    none + rest

  def render(acc: Account, allAccounts: List[Account]): String =
    val opts         = parentOptions(allAccounts, acc.account, acc.id)
    val checkedDebit = if (acc.isDebit) " checked" else ""
    val checkedBs    = if (acc.balancesheet) " checked" else ""

    s"""
       |<form id="account-form"
       |      class="htmx-grid"
       |      hx-put="/html/account"
       |      hx-target="#account-form"
       |      hx-swap="outerHTML">
       |
       |  <input type="hidden" name="id" value="${esc(acc.id)}" />
       |  <input type="hidden" name="modelid" value="${acc.modelid}" />
       |  <input type="hidden" name="company" value="${esc(acc.company)}" />
       |
       |  <!-- Row 1: Id | Name -->
       |  <label class="label" for="account-id">Id</label>
       |  <input id="account-id" class="input input-xs w-full" name="id_display"
       |         value="${esc(acc.id)}" readonly />
       |  <label class="label" for="account-name">Name</label>
       |  <input id="account-name" class="input input-xs w-full" name="name"
       |         value="${esc(acc.name)}" />
       |
       |  <!-- Row 2: Parent (cols 3-4 empty) -->
       |  <label class="label" for="account-parent">Parent</label>
       |  <select id="account-parent" class="select select-xs w-full" name="account">
       |    $opts
       |  </select>
       |
       |  <!-- Row 3: Description | Currency -->
       |  <label class="label" for="account-desc">Description</label>
       |  <textarea id="account-desc" class="textarea textarea-xs w-full" name="description">${esc(acc.description)}</textarea>
       |  <label class="label" for="account-currency">Currency</label>
       |  <input id="account-currency" class="input input-xs w-full" name="currency"
       |         value="${esc(acc.currency)}" />
       |
       |  <!-- Row 4: Is debit | Balance sheet -->
       |  <label class="label" for="account-debit">Is debit</label>
       |  <input id="account-debit" type="checkbox" class="checkbox checkbox-xs"
       |         name="isDebit"$checkedDebit />
       |  <label class="label" for="account-bs">Balance sheet</label>
       |  <input id="account-bs" type="checkbox" class="checkbox checkbox-xs"
       |         name="balancesheet"$checkedBs />
       |
       |  <!-- Row 5: Save -->
       |  <span></span>
       |  <button type="submit" class="btn btn-primary btn-xs justify-self-start">Save</button>
       |  <span></span>
       |  <span></span>
       |</form>
       |""".stripMargin