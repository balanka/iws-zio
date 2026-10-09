package com.kabasoft.iws.api

object HtmxToolbar:

  def render(prefix: String, modelid: Int, company: String): String =
    s"""
       |<div class="card bg-base-100 shadow-sm">
       |  <div class="card-body compact py-1">
       |    <div class="htmx-toolbar">
       |
       |      <button type="button"
       |              class="btn btn-xs btn-outline"
       |              hx-get="/html/$prefix/new/$modelid/$company"
       |              hx-target="#$prefix-form"
       |              hx-swap="outerHTML"
       |              title="Create a new record">
       |        <span>New</span>
       |      </button>
       |
       |      <button type="button"
       |              class="btn btn-xs btn-outline"
       |              id="toggle-edit-btn"
       |              data-edit-target="#$prefix-form"
       |              title="Toggle edit mode">
       |        <span class="edit-label">Edit</span>
       |      </button>
       |
       |      <button type="button"
       |              class="btn btn-xs btn-primary"
       |              hx-put="/html/$prefix"
       |              hx-include="#$prefix-form"
       |              hx-target="#$prefix-form"
       |              hx-swap="outerHTML"
       |              title="Save the form">
       |        <span>Save</span>
       |      </button>
       |
       |      <span class="toolbar-sep"></span>
       |
       |      <button type="button"
       |              class="btn btn-xs btn-outline"
       |              data-toggle="#$prefix-form-card"
       |              data-label-show="Show Form"
       |              data-label-hide="Hide Form"
       |              title="Show / hide the form">
       |        <span class="toggle-arrow">▾</span>
       |        <span class="toggle-label">Hide Form</span>
       |      </button>
       |
       |      <button type="button"
       |              class="btn btn-xs btn-outline"
       |              data-toggle="#$prefix-list-card"
       |              data-label-show="Show List"
       |              data-label-hide="Hide List"
       |              title="Show / hide the list">
       |        <span class="toggle-arrow">▾</span>
       |        <span class="toggle-label">Hide List</span>
       |      </button>
       |
       |      <button type="button"
       |              class="btn btn-xs btn-outline"
       |              id="toggle-search-btn"
       |              title="Show / hide the search field">
       |        <span class="toggle-arrow">▸</span>
       |        <span class="toggle-label">Show Search</span>
       |      </button>
       |
       |      <label class="page-size ms-auto">
       |        <span>Density</span>
       |        <select id="density-select" class="select select-xs density-select">
       |          <option value="xdense">Extra dense</option>
       |          <option value="compact">Compact</option>
       |          <option value="normal">Normal</option>
       |          <option value="spacious">Spacious</option>
       |        </select>
       |      </label>
       |
       |    </div>
       |  </div>
       |</div>
       |""".stripMargin