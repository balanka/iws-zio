package com.kabasoft.iws.api

import java.math.BigDecimal as JBigDecimal
import java.text.NumberFormat
import java.util.{Currency, Locale}

object Format:

  /**
   * App-wide locale for number separators only.
   *   Locale.GERMANY → "1.234,56"
   *   Locale.US      → "1,234.56"
   *   Locale.FRANCE  → "1 234,56"
   *
   * Currency symbol placement is always "amount SYMBOL"
   * regardless of locale, to match the form layout.
   */
  private val AppLocale: Locale = Locale.GERMANY

  /** "1.234,56 €" — amount, space, symbol. */
  def money(amount: JBigDecimal, currency: String): String =
    s"${decimal(amount)} ${symbol(currency)}"

  /** "1.234,56" — no symbol. */
  def decimal(amount: JBigDecimal): String =
    val fmt = NumberFormat.getNumberInstance(AppLocale)
    fmt.setMinimumFractionDigits(2)
    fmt.setMaximumFractionDigits(2)
    fmt.setGroupingUsed(true)
    fmt.format(amount)

  /** Symbol only, e.g. "€", "$", "GNF". */
  def symbol(currency: String): String =
    try Currency.getInstance(currency).getSymbol(AppLocale)
    catch case _: IllegalArgumentException => currency