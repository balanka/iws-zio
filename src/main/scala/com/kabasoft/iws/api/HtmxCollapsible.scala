package com.kabasoft.iws.api

object HtmxCollapsible:

  def section(
               title: String,
               id: String,
               open: Boolean = true,
               content: String,
               titleId: String = ""
             ): String =
    val titleIdAttr = if titleId.nonEmpty then s""" id="${Html.esc(titleId)}"""" else ""
    s"""
       |<details id="$id" class="collapsible-section" ${if open then "open" else ""}>
       |  <summary class="collapsible-summary">
       |    <span class="collapsible-arrow" aria-hidden="true"></span>
       |    <span class="collapsible-title"$titleIdAttr>${Html.esc(title)}</span>
       |  </summary>
       |  <div class="collapsible-content">
       |    $content
       |  </div>
       |</details>
       |""".stripMargin