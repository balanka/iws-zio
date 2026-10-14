package com.kabasoft.iws.api

object HtmxCollapsible:

  /** Native <details>/<summary> section.
   *  - Fields stay submitted when collapsed (native <details> behaviour).
   *  - Works inside an HTMX hx-put form with no JS changes.
   *  - Persists open/closed state and auto-opens on invalid via pageScript.
   */
  def section(
               title:   String,
               id:      String,
               open:    Boolean = false,
               content: String
             ): String =
    val openAttr = if open then " open" else ""
    s"""
       |<details id="${Html.esc(id)}" class="collapsible-section"$openAttr>
       |  <summary class="collapsible-summary">
       |    <span class="collapsible-arrow" aria-hidden="true"></span>
       |    <span class="collapsible-title">${Html.esc(title)}</span>
       |  </summary>
       |  <div class="collapsible-content">
       |    $content
       |  </div>
       |</details>
       |""".stripMargin