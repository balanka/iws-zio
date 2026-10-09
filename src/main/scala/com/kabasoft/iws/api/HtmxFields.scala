package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{Account, Contact, Masterfile, Vat}
import Html.esc

/**
 * Reusable field renderers for the masterfile forms.
 * Every dropdown option list starts with a "— none —" entry.
 */
object HtmxFields:

  // ── Low-level: option list ─────────────────────────────────────────────

  private def options[A](
                          items:    List[A],
                          selected: String,
                          value:    A => String,
                          label:    A => String
                        ): String =
    val noneSel = if selected == "-1" || selected.isEmpty then " selected" else ""
    val none    = s"""<option value="-1"$noneSel>— none —</option>"""
    val rest = items.map { it =>
      val sel = if value(it) == selected then " selected" else ""
      s"""<option value="${esc(value(it))}"$sel>${esc(label(it))}</option>"""
    }.mkString
    none + rest

  // ── Accounts ───────────────────────────────────────────────────────────

  def accountOptions(accounts: List[Account], selected: String): String =
    options(accounts, selected, _.id, a => s"${a.id} ${a.name}")

  def accountSelect(
                     id:       String,
                     name:     String,
                     accounts: List[Account],
                     selected: String,
                     disabled: Boolean = false
                   ): String =
    val dis = if disabled then " disabled" else ""
    s"""<select id="$id" class="select select-xs w-full" name="$name"$dis>
       |  ${accountOptions(accounts, selected)}
       |</select>""".stripMargin

  def accountRow(
                  labelTitle: String,
                  id:         String,
                  name:       String,
                  accounts:   List[Account],
                  selected:   String,
                  disabled:   Boolean = false
                ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |<select id="$id" class="select select-xs w-full" name="$name"${if disabled then " disabled" else ""}>
       |  ${accountOptions(accounts, selected)}
       |</select>""".stripMargin

  // ── VAT ────────────────────────────────────────────────────────────────

  def vatOptions(vats: List[Vat], selected: String): String =
    options(vats, selected, _.id, v => s"${v.id} ${v.name}")

  def vatSelect(
                 id:       String,
                 name:     String,
                 vats:     List[Vat],
                 selected: String,
                 disabled: Boolean = false
               ): String =
    val dis = if disabled then " disabled" else ""
    s"""<select id="$id" class="select select-xs w-full" name="$name"$dis>
       |  ${vatOptions(vats, selected)}
       |</select>""".stripMargin

  def vatRow(
              labelTitle: String,
              id:         String,
              name:       String,
              vats:       List[Vat],
              selected:   String,
              disabled:   Boolean = false
            ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |<select id="$id" class="select select-xs w-full" name="$name"${if disabled then " disabled" else ""}>
       |  ${vatOptions(vats, selected)}
       |</select>""".stripMargin

  def hiddenField(name: String, value: String): String =
         s"""<input type="hidden" name="$name" value="${esc(value)}" />"""
  // ── Contacts ───────────────────────────────────────────────────────────

  def contactOptions(contacts: List[Contact], selected: String): String =
    options(contacts, selected, _.id, c => s"${c.id} ${c.name}")

  def contactSelect(
                     id:       String,
                     name:     String,
                     contacts: List[Contact],
                     selected: String,
                     disabled: Boolean = false
                   ): String =
    val dis = if disabled then " disabled" else ""
    s"""<select id="$id" class="select select-xs w-full" name="$name"$dis>
       |  ${contactOptions(contacts, selected)}
       |</select>""".stripMargin

  def contactRow(
                  labelTitle: String,
                  id:         String,
                  name:       String,
                  contacts:   List[Contact],
                  selected:   String,
                  disabled:   Boolean = false
                ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |<select id="$id" class="select select-xs w-full" name="$name"${if disabled then " disabled" else ""}>
       |  ${contactOptions(contacts, selected)}
       |</select>""".stripMargin

  // ── Currency ───────────────────────────────────────────────────────────

  private val commonCurrencies: List[String] =
    List("EUR", "USD", "GBP", "CHF", "GNF", "XOF", "XAF", "MAD", "DZD", "TND", "CAD", "JPY")

  def currencyOptions(selected: String): String =
    if selected.isEmpty then ""
    else
      val hasSel = commonCurrencies.contains(selected)
      val head =
        if hasSel then ""
        else s"""<option value="${esc(selected)}" selected>${esc(selected)}</option>"""
      val rest = commonCurrencies.map { c =>
        val sel = if c == selected then " selected" else ""
        s"""<option value="$c"$sel>$c</option>"""
      }.mkString
      head + rest

  def currencySelect(
                      id:       String,
                      name:     String,
                      selected: String,
                      disabled: Boolean = false
                    ): String =
    val dis = if disabled then " disabled" else ""
    s"""<select id="$id" class="select select-xs w-full" name="$name"$dis>
       |  ${currencyOptions(selected)}
       |</select>""".stripMargin

  def currencyRow(
                   labelTitle: String,
                   id:         String,
                   name:       String,
                   selected:   String,
                   disabled:   Boolean = false
                 ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |<select id="$id" class="select select-xs w-full" name="$name"${if disabled then " disabled" else ""}>
       |  ${currencyOptions(selected)}
       |</select>""".stripMargin

  // ── Money input with currency suffix ───────────────────────────────────

  def moneyInput(
                  id:       String,
                  name:     String,
                  value:    String,
                  currency: String
                ): String =
    val sym = Format.symbol(currency)
    s"""<div class="input-money">
       |  <input id="$id" class="input input-xs num" name="$name"
       |         type="number" step="0.01" value="$value" />
       |  <span class="currency-suffix">${esc(sym)}</span>
       |</div>""".stripMargin

  def moneyRow(
                labelTitle: String,
                id:         String,
                name:       String,
                value:      String,
                currency:   String
              ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |${moneyInput(id, name, value, currency)}""".stripMargin

  def numberRow(
                 labelTitle: String,
                 id: String,
                 name: String,
                 value: String
               ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |<input id="$id" class="input input-xs num" name="$name"
       |       type="number" step="0.01" value="$value" />""".stripMargin

  // ── Text / textarea / checkbox ─────────────────────────────────────────

  def textInput(
                 id:       String,
                 name:     String,
                 value:    String,
                 readonly: Boolean = false
               ): String =
    val ro = if readonly then " readonly" else ""
    s"""<input id="$id" class="input input-xs w-full" name="$name" value="${esc(value)}"$ro />"""

  def textRow(
               labelTitle: String,
               id:         String,
               name:       String,
               value:      String,
               readonly:   Boolean = false
             ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |${textInput(id, name, value, readonly)}""".stripMargin

  def textarea(
                id:    String,
                name:  String,
                value: String
              ): String =
    s"""<textarea id="$id" class="textarea textarea-xs w-full" name="$name">${esc(value)}</textarea>"""

  def textareaRow(
                   labelTitle: String,
                   id:         String,
                   name:       String,
                   value:      String
                 ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |${textarea(id, name, value)}""".stripMargin

  def checkbox(
                id:      String,
                name:    String,
                checked: Boolean
              ): String =
    val ck = if checked then " checked" else ""
    s"""<input id="$id" type="checkbox" class="checkbox checkbox-xs" name="$name"$ck />"""

  def checkboxRow(
                   labelTitle: String,
                   id:         String,
                   name:       String,
                   checked:    Boolean
                 ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |${checkbox(id, name, checked)}""".stripMargin

  // ── Button ─────────────────────────────────────────────────────────────

  def saveRow: String =
    s"""<span></span>
       |<button type="submit" class="btn btn-primary btn-xs justify-self-start">Save</button>
       |<span></span>
       |<span></span>""".stripMargin

  def masterfileOptions(items: List[Masterfile], selected: String): String =
    options(items, selected, _.id, m => s"${m.id} ${m.name}")

  def masterfileSelect(
                        id: String,
                        name: String,
                        items: List[Masterfile],
                        selected: String,
                        disabled: Boolean = false
                      ): String =
    val dis = if disabled then " disabled" else ""
    s"""<select id="$id" class="select select-xs w-full" name="$name"$dis>
       |  ${masterfileOptions(items, selected)}
       |</select>""".stripMargin

  def masterfileRow(
                     labelTitle: String,
                     id: String,
                     name: String,
                     items: List[Masterfile],
                     selected: String,
                     disabled: Boolean = false
                   ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |<select id="$id" class="select select-xs w-full" name="$name"${if disabled then " disabled" else ""}>
       |  ${masterfileOptions(items, selected)}
       |</select>""".stripMargin

  def choiceOptions(choices: List[(String, String)], selected: String): String =
    choices.map { case (value, label) =>
      val sel = if value == selected then " selected" else ""
      s"""<option value="${esc(value)}"$sel>${esc(label)}</option>"""
    }.mkString

  def choiceSelect(
                    id: String,
                    name: String,
                    choices: List[(String, String)],
                    selected: String
                  ): String =
    s"""<select id="$id" class="select select-xs w-full" name="$name">
       |  ${choiceOptions(choices, selected)}
       |</select>""".stripMargin

  def choiceRow(
                 labelTitle: String,
                 id: String,
                 name: String,
                 choices: List[(String, String)],
                 selected: String
               ): String =
    s"""<label class="label" for="$id">${esc(labelTitle)}</label>
       |<select id="$id" class="select select-xs w-full" name="$name">
       |  ${choiceOptions(choices, selected)}
       |</select>""".stripMargin

  /** The permission right codes used across the app. */
  val permissionRights: List[(String, String)] = List(
    ("", "— none —"),
    ("r", "r — read"),
    ("w", "w — write"),
    ("p", "p — print"),
    ("t", "t — transmit"),
    ("+", "+ — add"),
    ("-", "- — delete")
  )