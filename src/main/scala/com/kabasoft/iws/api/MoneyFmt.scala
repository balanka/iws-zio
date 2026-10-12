package com.kabasoft.iws.api

import java.text.NumberFormat
import java.util.{Currency, Locale}

object MoneyFmt:

  /** Currency symbol for a 3-letter ISO code.
   * Falls back to the code itself if the code is unknown.
   */
  def symbol(code: String): String =
    Option(code).map(_.trim).filter(_.nonEmpty) match
      case None => ""
      case Some(c) =>
        try Currency.getInstance(c.toUpperCase).getSymbol(Locale.GERMANY)
        catch case _: Throwable => c

  /** Formats an amount as "1.234,56 €" (or the matching symbol).
   * Uses German locale for thousand / decimal separators.
   */
  def format(amount: java.math.BigDecimal, code: String): String =
    val nf = NumberFormat.getNumberInstance(Locale.GERMANY)
    nf.setMinimumFractionDigits(2)
    nf.setMaximumFractionDigits(2)
    val num    = nf.format(amount)
    val suffix = symbol(code)
    if suffix.isEmpty then num else s"$num $suffix"