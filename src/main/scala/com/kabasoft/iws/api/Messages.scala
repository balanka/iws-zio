package com.kabasoft.iws.api

object Messages:

  /** Two-letter codes we support. Extend as needed. */
  val supported: List[String] = List("en", "de", "fr")
  val default:   String       = "en"

  /** Parse Accept-Language (or a lang cookie) and return the best match. */
  def langFrom(header: Option[String]): String =
    header.flatMap { h =>
      h.split(",").iterator
        .map(_.trim.split(";")(0).trim.toLowerCase)
        .map(tag => tag.split("-")(0))
        .find(supported.contains)
    }.getOrElse(default)

  /** modelid -> lang -> title */
  private val masterfileTitles: Map[Int, Map[String, String]] = Map(
    6   -> Map("en" -> "Cost Center",   "de" -> "Kostenstelle",     "fr" -> "Centre de coût"),
    15  -> Map("en" -> "Quantity Unit", "de" -> "Mengeneinheit",    "fr" -> "Unité de quantité"),
    99  -> Map("en" -> "Currency",      "de" -> "Währung",          "fr" -> "Devise"),
    36  -> Map("en" -> "Account Class", "de" -> "Kontoklasse",      "fr" -> "Classe de compte"),
    121 -> Map("en" -> "Role",          "de" -> "Rolle",            "fr" -> "Rôle")
  )

  def masterfileTitle(modelid: Int, lang: String): String =
    masterfileTitles
      .get(modelid)
      .flatMap(m => m.get(lang).orElse(m.get(default)))
      .getOrElse(s"Masterfile $modelid")