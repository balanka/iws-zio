package com.kabasoft.iws.api

object HtmxFormIndex:

  /**
   * Parse indexed sub-grid fields into a list of row maps.
   *
   * Given params like:
   *   "bankaccounts.0.id" -> "b1"
   *   "bankaccounts.0.bic" -> "XXXX"
   *   "bankaccounts.2.id" -> "b2"
   *
   * `parseIndexed(params, "bankaccounts")` returns:
   *   List(Map("id" -> "b1", "bic" -> "XXXX"),
   *        Map("id" -> "b2"))
   *
   * Rows are ordered by their numeric index. Gaps (missing index 1) are fine.
   */
  def parseIndexed(params: Map[String, String], prefix: String): List[Map[String, String]] =
    val pattern = ("""^""" + java.util.regex.Pattern.quote(prefix) + """\.(\d+)\.(.+)$""").r
    params.toList
      .flatMap {
        case (pattern(idx, key), value) => Some((idx.toInt, key, value))
        case _                          => None
      }
      .groupBy(_._1)
      .toList
      .sortBy(_._1)
      .map { case (_, entries) =>
        entries.map { case (_, k, v) => k -> v }.toMap
      }