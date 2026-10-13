package com.kabasoft.iws.api

/** The wrapper every combined page shares: page container, toolbar,
 * form card, list card. Callers render each piece and hand the HTML in.
 *
 * The list card's `.card-content` wrapper is required by the collapse CSS
 * (`.card.collapsed > .card-body > *:not(.card-header-row) { display: none; }`)
 * and by the fmodule combo's `hx-target="#<prefix>-list-card .card-content"`.
 * Keeping the wrapper here prevents the two combined pages drifting apart.
 */
object HtmxCombinedLayout:

  def render(
              prefix:      String,
              title:       String,
              toolbarHtml: String,
              formHtml:    String,
              listHtml:    String
            ): String =
    s"""
       |<div class="container mx-auto max-w-5xl">
       |  <div class="htmx-page-wrapper">
       |
       |    $toolbarHtml
       |
       |    <div id="$prefix-form-card" class="card bg-base-100 shadow-sm">
       |      <div class="card-body compact">
       |        <div class="card-header-row">
       |          <h2 class="card-title">${Html.esc(title)}</h2>
       |          <button type="button" class="btn btn-ghost btn-xs"
       |                  data-toggle="#$prefix-form-card"
       |                  data-label-show="Show" data-label-hide="Hide">
       |            <span class="toggle-arrow">▾</span>
       |            <span class="toggle-label">Hide</span>
       |          </button>
       |        </div>
       |        <div class="card-content">$formHtml</div>
       |      </div>
       |    </div>
       |
       |    <div id="$prefix-list-card" class="card bg-base-100 shadow-sm">
       |      <div class="card-body compact">
       |        <div class="card-content">$listHtml</div>
       |      </div>
       |    </div>
       |
       |  </div>
       |</div>
       |""".stripMargin