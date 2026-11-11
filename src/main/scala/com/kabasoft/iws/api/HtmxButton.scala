package com.kabasoft.iws.api

object HtmxButtons:

  /**
   * Renders a toolbar icon button matching the React `ToolbarButton`
   * (CoreUI + MUI IconButton look) but in plain HTML.
   *
   * @param tooltip  text shown in the tooltip / title attribute
   * @param action   raw HTML attributes to attach (hx-get, onclick, etc.)
   * @param icon     inline SVG or icon HTML (e.g. CoreUI <svg class="icon">)
   * @param disabled if true, renders the button disabled
   * @param id       optional id for JS handlers
   */

  def iconButton(
                  tooltip: String,
                  action: String = "",
                  icon: String,
                  disabled: Boolean = false,
                  id: String = "",
                  buttonType: String = "button", // ← new
                  formTarget: String = "", // ← new
                  extraClass: String = "" // ← new, for "toolbar-btn-primary"
                ): String =
    val idAttr = if id.nonEmpty then s"""id="$id"""" else ""
    val dis = if disabled then " disabled" else ""
    val formAttr = if formTarget.nonEmpty then s"""form="$formTarget"""" else ""
    val cls = s"toolbar-btn${if extraClass.nonEmpty then " " + extraClass else ""}${if disabled then " toolbar-btn-disabled" else ""}"
    s"""<button type="$buttonType"
       |        class="$cls"
       |        $idAttr
       |        $formAttr
       |        title="${Html.esc(tooltip)}"
       |        $action
       |        $dis>
       |  $icon
       |</button>""".stripMargin

  /**
   * Renders a labelled toolbar button (with text label, optional icon).
   * Used for the toggle buttons (Hide Form, Show Search, etc.) that have
   * both an arrow and a label.
   */

  def labelledButton(
                      label:       String,
                      action:      String = "",
                      id:          String = "",
                      extraClass:  String = "",
                      title:       String = "",
                      ariaLabel:   String = "",
                      icon:        String = ""      // ← new
                    ): String =
              val idAttr = if id.nonEmpty then s"""id="$id"""" else ""
              val tt     = if title.nonEmpty then s"""title="${Html.esc(title)}"""" else ""
              val aria   = if ariaLabel.nonEmpty then s"""aria-label="${Html.esc(ariaLabel)}"""" else ""
              val iconHtml = if icon.nonEmpty then icon else """<span class="toggle-arrow" aria-hidden="true">▸</span>"""
              s"""<button type="button"
              |        class="toolbar-btn toolbar-btn-labelled ${Html.esc(extraClass)}"
              |        $idAttr
              |        $tt
              |        $aria
              |        $action>
              |  $iconHtml
              |  <span class="toggle-daisy-label">${Html.esc(label)}</span>
              |</button>""".stripMargin

