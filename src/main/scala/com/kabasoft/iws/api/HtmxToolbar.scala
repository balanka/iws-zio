package com.kabasoft.iws.api

object HtmxToolbar:

  def render(
              prefix:         String,
              modelid:        Int,
              company:        String,
              newFormUrl:     String = "",
              newFormInclude: String = "",
              showEdit:       Boolean = true
            ): String =

    val newAction =
      if newFormUrl.nonEmpty then
        s"""hx-get="$newFormUrl" hx-include="$newFormInclude" hx-target="#$prefix-form" hx-swap="outerHTML""""
      else
        s"""hx-get="/html/$prefix/new-form/$modelid/$company" hx-target="#$prefix-form" hx-swap="outerHTML""""

    val newBtn = HtmxButtons.iconButton(
      tooltip = "Create a new record",
      action  = newAction,
      icon    = MuiIcons.addIcon)

    val editBtn =
      if showEdit then
        HtmxButtons.iconButton(
          tooltip = "Toggle edit mode",
          id      = "toggle-edit-btn",
          action  = s"""data-edit-target="#$prefix-form"""",
          icon    = MuiIcons.editIcon)
      else ""

    val saveBtn = HtmxButtons.iconButton(
      tooltip    = "Save the form",
      icon       = MuiIcons.saveIcon,
      buttonType = "submit",
      formTarget = s"$prefix-form",
      extraClass = "toolbar-btn-primary"
    )

    val addLineBtn = HtmxButtons.iconButton(
      tooltip = "Add a new line to the active grid",
      id      = "add-line-btn",
      action  = """onclick="window.iwsAddLineToActiveGrid && window.iwsAddLineToActiveGrid()"""",
      icon    = MuiIcons.addLineIcon
    )

    val printBtn = HtmxButtons.iconButton(
      tooltip = "Print this record",
      action  = s"""onclick="window.iwsPrintForm && window.iwsPrintForm('$prefix')"""",
      icon    = MuiIcons.localPrinterIcon
    )

    val searchBtn = HtmxButtons.labelledButton(
      label = "Show Search",
      id    = "toggle-search-btn",
      title = "Show / hide the search field",
      icon  = MuiIcons.searchIcon)

    val listBtn = HtmxButtons.labelledButton(
      label = "Hide List",
      id    = "hide-list-btn",
      title = "Show / hide the list",
      icon  = MuiIcons.listIcon1)

    s"""
       |<div class="daisy-card bg-base-100 shadow-sm">
       |  <div class="daisy-card-body compact py-1">
       |    <div class="htmx-toolbar"
       |      hx-on:click="
       |        var t = event.target.closest('button');
       |        if (!t) return;
       |
       |        if (t.id === 'hide-form-btn') {
       |          var f = document.getElementById('${prefix}-form');
       |          if (f) {
       |            f.classList.toggle('hidden');
       |            var lbl = t.querySelector('.toggle-daisy-label');
       |            if (lbl) lbl.textContent = f.classList.contains('hidden') ? 'Show Form' : 'Hide Form';
       |          }
       |        }
       |
       |        if (t.id === 'hide-list-btn') {
       |          var l = document.getElementById('${prefix}-list-card');
       |          if (l) {
       |            l.classList.toggle('hidden');
       |            var lbl = t.querySelector('.toggle-daisy-label');
       |            if (lbl) lbl.textContent = l.classList.contains('hidden') ? 'Show List' : 'Hide List';
       |          }
       |        }
       |
       |        if (t.id === 'toggle-search-btn') {
       |          var s = document.getElementById('daisy-table-search-wrapper');
       |          if (s) {
       |            s.classList.toggle('hidden');
       |            t.classList.toggle('is-open');
       |          }
       |        }
       |      ">
       |
       |      <!-- Left group: action buttons -->
       |      $newBtn
       |      $addLineBtn
       |      $editBtn
       |      $saveBtn
       |      $printBtn
       |
       |      <!-- Right group: toggles + density -->
       |      <div class="toolbar-right-group ms-auto">
       |        $listBtn
       |        $searchBtn
       |        <label class="page-size">
       |          <span>Density</span>
       |          <select id="density-select" class="select daisy-select-xs density-daisy-select">
       |            <option value="xdense">Extra dense</option>
       |            <option value="compact">Compact</option>
       |            <option value="normal">Normal</option>
       |            <option value="spacious">Spacious</option>
       |          </select>
       |        </label>
       |      </div>
       |
       |    </div>
       |  </div>
       |</div>
       |""".stripMargin