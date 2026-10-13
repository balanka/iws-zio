package com.kabasoft.iws.api

object HtmxCss:

  val grid: String =
    """
      |    /* ==========================================
      |       Density variables
      |       ========================================== */
      |    :root {
      |      --grid-row-gap:      3px;
      |      --grid-col-gap:      8px;
      |      --grid-label-size:   12px;
      |      --grid-input-height: 26px;
      |      --grid-textarea-min: 34px;
      |      --card-pad:          8px 10px;
      |      --card-title-size:   15px;
      |      --card-title-mb:     6px;
      |      --table-cell-pad:    2px 6px;
      |      --table-font:        12px;
      |      --toolbar-gap:       6px;
      |      --pager-gap:         8px;
      |      --pager-mt:          4px;
      |      --btn-height:        24px;
      |      --btn-pad:           0 10px;
      |      --btn-font:          12px;
      |      --wrapper-spacing:   0.5rem;
      |      --wrapper-pad:       0.75rem;
      |    }
      |
      |    html[data-density="xdense"] {
      |      --grid-row-gap:      1px;
      |      --grid-col-gap:      5px;
      |      --grid-label-size:   11px;
      |      --grid-input-height: 22px;
      |      --grid-textarea-min: 28px;
      |      --card-pad:          4px 8px;
      |      --card-title-size:   13px;
      |      --card-title-mb:     3px;
      |      --table-cell-pad:    1px 4px;
      |      --table-font:        11px;
      |      --toolbar-gap:       4px;
      |      --pager-gap:         6px;
      |      --pager-mt:          2px;
      |      --btn-height:        20px;
      |      --btn-pad:           0 8px;
      |      --btn-font:          11px;
      |      --wrapper-spacing:   0.25rem;
      |      --wrapper-pad:       0.5rem;
      |    }
      |
      |    html[data-density="normal"] {
      |      --grid-row-gap:      6px;
      |      --grid-col-gap:      12px;
      |      --grid-label-size:   13px;
      |      --grid-input-height: 30px;
      |      --grid-textarea-min: 50px;
      |      --card-pad:          12px 16px;
      |      --card-title-size:   17px;
      |      --card-title-mb:     10px;
      |      --table-cell-pad:    4px 8px;
      |      --table-font:        13px;
      |      --toolbar-gap:       8px;
      |      --pager-gap:         12px;
      |      --pager-mt:          8px;
      |      --btn-height:        28px;
      |      --btn-pad:           0 12px;
      |      --btn-font:          13px;
      |      --wrapper-spacing:   0.75rem;
      |      --wrapper-pad:       1rem;
      |    }
      |
      |    html[data-density="spacious"] {
      |      --grid-row-gap:      10px;
      |      --grid-col-gap:      16px;
      |      --grid-label-size:   14px;
      |      --grid-input-height: 36px;
      |      --grid-textarea-min: 72px;
      |      --card-pad:          16px 20px;
      |      --card-title-size:   18px;
      |      --card-title-mb:     14px;
      |      --table-cell-pad:    6px 10px;
      |      --table-font:        14px;
      |      --toolbar-gap:       10px;
      |      --pager-gap:         14px;
      |      --pager-mt:          12px;
      |      --btn-height:        32px;
      |      --btn-pad:           0 14px;
      |      --btn-font:          14px;
      |      --wrapper-spacing:   1rem;
      |      --wrapper-pad:       1.5rem;
      |    }
      |
      |    /* ==========================================
      |       Page wrapper
      |       ========================================== */
      |    .htmx-page-wrapper {
      |      display: flex;
      |      flex-direction: column;
      |      gap: var(--wrapper-spacing);
      |      padding: var(--wrapper-pad);
      |    }
      |
      |    /* ==========================================
      |       Form grid — 4 columns
      |       ========================================== */
      |    .htmx-grid {
      |      display: grid;
      |      grid-template-columns: max-content 1fr max-content 1fr;
      |      column-gap: var(--grid-col-gap);
      |      row-gap:    var(--grid-row-gap);
      |      align-items: center;
      |      background: #fafafa;
      |      padding: 8px;
      |      border-radius: 6px;
      |    }
      |    .htmx-grid > input[type="hidden"] { display: none; }
      |    .htmx-grid > .label {
      |      white-space: nowrap;
      |      justify-self: start;
      |      padding: 0;
      |      font-size: var(--grid-label-size);
      |      line-height: 1.2;
      |    }
      |    .htmx-grid > textarea { min-height: var(--grid-textarea-min); resize: vertical; }
      |
      |    .htmx-grid > .input,
      |    .htmx-grid > .select,
      |    .htmx-grid > .textarea {
      |      height: var(--grid-input-height);
      |      padding: 2px 6px;
      |      font-size: var(--grid-label-size);
      |    }
      |    .htmx-grid > .textarea { height: auto; }
      |    .htmx-grid > .btn {
      |      height: var(--grid-input-height);
      |      min-height: var(--grid-input-height);
      |      padding: var(--btn-pad);
      |      font-size: var(--grid-label-size);
      |    }
      |    .htmx-grid > .checkbox { height: 16px; width: 16px; }
      |
      |    /* ==========================================
      |       Numeric alignment
      |       ========================================== */
      |    .num { text-align: right; font-variant-numeric: tabular-nums; }
      |    .num-cell {
      |      text-align: right;
      |      font-variant-numeric: tabular-nums;
      |      padding-right: 8px !important;
      |    }
      |
      |    /* ==========================================
      |       Money fields — currency symbol overlaid
      |       inside the input / computed-cell box
      |       ========================================== */
      |    .input-money {
      |      position: relative;
      |      display: block;
      |      width: 100%;
      |    }
      |    .input-money > .input,
      |    .input-money > .calc-cell {
      |      width: 100%;
      |      padding-right: 32px;        /* room for the symbol */
      |      text-align: right;
      |      box-sizing: border-box;
      |    }
      |    .input-money > .currency-suffix {
      |      position: absolute;
      |      right: 8px;
      |      top: 50%;
      |      transform: translateY(-50%);
      |      pointer-events: none;
      |      user-select: none;
      |      font-size: var(--grid-label-size);
      |      line-height: 1;
      |      color: #666;
      |      white-space: nowrap;
      |    }
      |
      |    /* ==========================================
      |       Toolbar
      |       ========================================== */
      |    .htmx-toolbar {
      |      display: flex;
      |      flex-wrap: wrap;
      |      align-items: center;
      |      gap: var(--toolbar-gap);
      |    }
      |    .htmx-toolbar .btn { min-width: 100px; justify-content: center; }
      |    .htmx-toolbar .density-select { width: auto; min-width: 100px; }
      |    .toolbar-sep {
      |      display: inline-block;
      |      width: 1px;
      |      height: 18px;
      |      background: #ddd;
      |      margin: 0 4px;
      |    }
      |
      |    /* ==========================================
      |       Collapsible cards
      |       ========================================== */
      |    .card-header-row {
      |      display: flex; justify-content: space-between; align-items: center;
      |      margin-bottom: var(--card-title-mb);
      |    }
      |    .card-header-row .card-title {
      |      margin: 0;
      |      font-size: var(--card-title-size);
      |      line-height: 1.2;
      |    }
      |    /* Card collapse — hide everything inside .card-body except
      |       the header row (which holds the collapse toggle). Works whether
      |       the body contains .card-content, .htmx-table-wrapper, or a
      |       dynamically swapped fragment. */
      |    .card.collapsed > .card-body > *:not(.card-header-row) { display: none; }
      |    .card.collapsed .card-header-row { margin-bottom: 0; }
      |    .toggle-arrow { font-size: 12px; color: #666; display: inline-block; width: 10px; }
      |
      |    .card-body.compact { padding: var(--card-pad); }
      |
      |    /* ==========================================
      |       Table wrapper
      |       ========================================== */
      |    .htmx-table-wrapper { width: 100%; }
      |
      |    body.hide-search .table-search-wrapper { display: none; }
      |
      |    .table-scroll {
      |      border: 1px solid #e5e7eb;
      |      border-radius: 4px;
      |      background: white;
      |    }
      |    .table-scroll table { margin: 0; }
      |    .table-scroll table td,
      |    .table-scroll table th {
      |      padding: var(--table-cell-pad);
      |      font-size: var(--table-font);
      |      line-height: 1.3;
      |    }
      |    .table-scroll thead th {
      |      position: sticky; top: 0; z-index: 2;
      |      background: var(--color-base-100, #fff);
      |      box-shadow: inset 0 -1px 0 #e5e7eb;
      |      font-size: var(--table-font);
      |    }
      |
      |        /* ==========================================
      |       Resizable columns
      |       ========================================== */
      |    table.htmx-table-fixed {
      |      table-layout: fixed;
      |      width: 100%;
      |    }
//      |    table.htmx-table-fixed th,
//      |    table.htmx-table-fixed td {
//      |      overflow: hidden;
//      |      text-overflow: ellipsis;
//      |      white-space: nowrap;
//      |    }
      |   table.htmx-table-fixed th {
      |     overflow: hidden;
      |     text-overflow: ellipsis;
      |     white-space: nowrap;
      |   }
      |  table.htmx-table-fixed td {
      |   overflow: hidden;
      |  }
      |  table.htmx-table-fixed td input,
      |  table.htmx-table-fixed td select {
      |     min-width: 0;
      |     width: 100%;
      | }
      |    /* Allow wrapping inside the resizable Text column so longer text
      |       doesn't get cut off. Marked by .th-text on the header. */
      |    table.htmx-table-fixed th.th-text,
      |    table.htmx-table-fixed th.th-text ~ td:nth-child(8) {
      |      white-space: normal;
      |      overflow-wrap: anywhere;
      |    }
      |
      |    .col-resizer {
      |      position: absolute;
      |      top: 0;
      |      right: -3px;
      |      width: 7px;
      |      height: 100%;
      |      cursor: col-resize;
      |      user-select: none;
      |      z-index: 3;
      |      background: transparent;
      |    }
      |    .col-resizer::after {
      |      content: "";
      |      position: absolute;
      |      top: 15%;
      |      bottom: 15%;
      |      right: 3px;
      |      width: 1px;
      |      background: #d0d0d0;
      |    }
      |    .col-resizer:hover::after,
      |    .col-resizer.active::after {
      |      background: #1976d2;
      |      width: 2px;
      |    }
      |    th.resizable-th {
      |      position: relative;
      |    }
      |    body.col-resizing,
      |    body.col-resizing * {
      |      cursor: col-resize !important;
      |      user-select: none !important;
      |    }
      |
      |    .table-row { cursor: pointer; }
      |    .table-row:hover { background-color: #e3f2fd !important; }
      |    .table-sort { color: inherit; text-decoration: none; }
      |    .table-sort:hover { color: #1976d2; }
      |
      |    /* ==========================================
      |       Boolean cells — checkbox
      |       ========================================== */
      |    .table-scroll td.bool-cell {
      |      text-align: center;
      |      vertical-align: middle;
      |    }
      |    .table-scroll td.bool-cell input[type="checkbox"] {
      |      pointer-events: none;
      |      margin: 0;
      |      vertical-align: middle;
      |      width: 14px;
      |      height: 14px;
      |      accent-color: #1976d2;
      |    }
      |
      |    /* ==========================================
      |       Table toolbar — search + filters
      |       ========================================== */
      |    .table-toolbar {
      |      display: flex;
      |      flex-wrap: wrap;
      |      align-items: center;
      |      gap: 8px;
      |      margin-bottom: 6px;
      |    }
      |    .table-search-wrapper { flex: 0 1 240px; }
      |
      |    /* ==========================================
      |       Auto-filter row under the table header
      |       ========================================== */
      |    .table-filter-row > th.table-filter-cell {
      |      position: sticky;
      |      top: calc(var(--table-font) + var(--table-cell-pad) * 2 + 6px);
      |      z-index: 2;
      |      background: var(--color-base-100, #fff);
      |      box-shadow: inset 0 -1px 0 #e5e7eb;
      |      padding: 2px 4px;
      |      vertical-align: middle;
      |    }
      |    .table-filter-input {
      |      width: 100%;
      |      height: calc(var(--grid-input-height) - 4px);
      |      padding: 1px 6px;
      |      font-size: var(--table-font);
      |      border: 1px solid #e5e7eb;
      |      border-radius: 3px;
      |      background: #fafafa;
      |    }
      |    .table-filter-input:focus {
      |      background: #fff;
      |      outline: none;
      |      border-color: #bcd9f5;
      |    }
      |    .table-filter-input.num { text-align: right; }
      |    .table-filter-cell select.table-filter-input {
      |      min-width: 0;
      |      width: 100%;
      |    }
      |
      |    /* ==========================================
      |       Pager
      |       ========================================== */
      |    .table-pager {
      |      display: grid;
      |      grid-template-columns: 1fr auto 1fr;
      |      align-items: center;
      |      gap: var(--pager-gap);
      |      margin-top: var(--pager-mt);
      |    }
      |    .pager-left  { justify-self: start; }
      |    .pager-right { justify-self: end; }
      |    .pager-center {
      |      display: flex; align-items: center; gap: var(--pager-gap);
      |      justify-self: center;
      |    }
      |    .page-size {
      |      display: flex; align-items: center; gap: 4px;
      |      font-size: var(--table-font); color: #666;
      |    }
      |    .page-size select {
      |      width: auto; min-width: 60px;
      |      height: var(--btn-height);
      |      padding: 0 4px;
      |      font-size: var(--table-font);
      |    }
      |    .page-info { font-size: var(--table-font); color: #666; }
      |    .table-pager .btn {
      |      height: var(--btn-height);
      |      min-height: var(--btn-height);
      |      padding: var(--btn-pad);
      |      font-size: var(--btn-font);
      |    }
      |
      |    /* ==========================================
      |       Sub-grid
      |       ========================================== */
      |    .htmx-subgrid { width: 100%; }
      |    .htmx-subgrid table { margin: 0; }
      |
      |    .htmx-subgrid th,
      |    .htmx-subgrid td {
      |      padding: var(--table-cell-pad);
      |      font-size: var(--table-font);
      |      line-height: 1.3;
      |    }
      |
      |    .htmx-subgrid thead th {
      |      background: var(--color-base-100, #fff);
      |      font-size: var(--table-font);
      |    }
      |
      |    .htmx-subgrid input.input-xs,
      |    .htmx-subgrid select.select-xs {
      |      height: var(--grid-input-height);
      |      width: 100%;
      |      padding: 2px 6px;
      |      font-size: var(--grid-label-size);
      |    }
      |
      |    /* Money overlay inside sub-grid cells — one-space gap */
      |    .htmx-subgrid .input-money > .input,
      |    .htmx-subgrid .input-money > .calc-cell {
      |      padding-right: 14px;
      |    }
      |    .htmx-subgrid .input-money > .currency-suffix {
      |      right: 3px;
      |      font-size: 10px;
      |      color: #888;
      |    }
      |
      |    /* Read-only cells inside an editable sub-grid */
      |    .htmx-subgrid input.fk-cell {
      |      background: #f0f0f0;
      |      color: #666;
      |      cursor: not-allowed;
      |    }
      |    .htmx-subgrid input.fk-cell:focus {
      |      outline: none;
      |      box-shadow: none;
      |    }
      |
      |    .htmx-subgrid .subgrid-actions {
      |      width: calc(var(--btn-height) + 8px);
      |      text-align: center;
      |    }
      |
      |    .htmx-subgrid .subgrid-actions .btn {
      |      height: var(--btn-height);
      |      min-height: var(--btn-height);
      |      padding: var(--btn-pad);
      |      font-size: var(--btn-font);
      |    }
      |
      |    .htmx-subgrid > button {
      |      height: var(--btn-height);
      |      min-height: var(--btn-height);
      |      padding: var(--btn-pad);
      |      font-size: var(--btn-font);
      |      margin-top: 4px;
      |    }
      |
      |    .htmx-subgrid .num {
      |      text-align: right;
      |    }
      |
      |    /* Entirely read-only sub-grid */
      |    .htmx-subgrid-readonly td {
      |      color: #444;
      |      background: transparent;
      |    }
      |    .htmx-subgrid-readonly .subgrid-empty {
      |      text-align: center;
      |      color: #999;
      |      font-style: italic;
      |    }
      |
      |   /* A textarea that stretches across columns 2..4 of the 4-column grid,
      |       leaving its label in column 1. Used for Text / Foot text in the
      |       header section. */
      |    .collapsible-content > textarea.span-value {
      |      grid-column: 2 / -1;
      |      width: 100%;
      |    }
      |
      |   /* Every value field in a collapsible header spans at most a fixed
      |       width so columns 2 and 4 look identical regardless of label length.
      |       Textareas marked `.span-value` are exempt (they fill 3 columns). */
      |    .collapsible-content > .input,
      |    .collapsible-content > .select {
      |      width: 100%;
      |      max-width: 240px;
      |    }
      |    /* Same cap for money/combo wrappers if used in the header later. */
      |    .collapsible-content > .input-money {
      |      max-width: 240px;
      |    }
      |    /* ==========================================
      |       Sub-grid block — spans the main form grid
      |       ========================================== */
      |    .subgrid-block {
      |      grid-column: 1 / -1;
      |      margin-top: 8px;
      |      padding-top: 8px;
      |      border-top: 1px solid #e5e7eb;
      |    }
      |    /* ==========================================
      |       Sub-grid total — badge with symbol inside
      |       ========================================== */
      |    .subgrid-total {
      |      display: flex;
      |      justify-content: flex-end;
      |      align-items: center;
      |      gap: 8px;
      |      margin-top: 4px;
      |      font-weight: 600;
      |    }
      |    .subgrid-total .total-money {
      |      position: relative;
      |      display: inline-block;
      |      min-width: 120px;
      |    }
      |    .subgrid-total .total-money > .calc-total {
      |      display: inline-block;
      |      width: 100%;
      |      min-width: 120px;
      |      padding: 2px 16px 2px 6px;   /* room on the right for the symbol */
      |      background: #eaf4fe;
      |      border: 1px solid #bcd9f5;
      |      border-radius: 4px;
      |      font-variant-numeric: tabular-nums;
      |      text-align: right;
      |      box-sizing: border-box;
      |    }
      |    .subgrid-total .total-money > .currency-suffix {
      |      position: absolute;
      |      right: 4px;
      |      top: 50%;
      |      transform: translateY(-50%);
      |      pointer-events: none;
      |      user-select: none;
      |      font-size: var(--table-font);
      |      line-height: 1;
      |      color: #666;
      |      white-space: nowrap;
      |    }
      |
      |    /* If a form uses the wrapper-div structure instead */
      |    .form-subgrid {
      |      margin-top: 12px;
      |      padding-top: 10px;
      |      border-top: 1px solid #e5e7eb;
      |    }
      |    .form-subgrid > .label {
      |      display: block;
      |      margin-bottom: 4px;
      |      font-weight: 500;
      |      color: #555;
      |      font-size: var(--grid-label-size);
      |    }
      |  /* Resizable subgrid headers keep the action column's compact width
      |       even when the table switches to fixed layout. */
      |    table.htmx-table-fixed th.subgrid-actions {
      |      width: calc(var(--btn-height) + 8px);
      |      text-align: center;
      |    }
      |    /* ==========================================
      |       Read-only form mode
      |       ========================================== */
      |    .form-readonly input:not([type="hidden"]):not([type="checkbox"]),
      |    .form-readonly select,
      |    .form-readonly textarea {
      |      pointer-events: none;
      |      background: #f5f5f5 !important;
      |      color: #666 !important;
      |      border-color: #e5e7eb !important;
      |      cursor: not-allowed;
      |    }
      |    .form-readonly input[type="checkbox"] {
      |      pointer-events: none;
      |      opacity: 0.55;
      |    }
      |    .form-readonly .htmx-subgrid .btn {
      |      pointer-events: none;
      |      opacity: 0.5;
      |    }
      |
      |    /* ==========================================
      |       Filter selects — always interactive
      |       (e.g. the Transaction / Financials module combo)
      |       ========================================== */
      |
      |    /* Behavioural override: apply everywhere. Only the pointer and
      |       opacity, so the combo stays usable when the form is read-only. */
      |    select.filter-select,
      |    .form-readonly select.filter-select,
      |    .htmx-form.form-readonly select.filter-select,
      |    .collapsible-content > select.filter-select,
      |    select.filter-select:disabled {
      |      pointer-events: auto !important;
      |      cursor: pointer !important;
      |      opacity: 1 !important;
      |    }
      |
      |    /* Visual override: apply in view mode only. In edit mode the combo
      |       looks like every other <select> (i.e. like Account). */
      |    .form-readonly select.filter-select,
      |    .htmx-form.form-readonly select.filter-select {
      |      background: #ffffff !important;
      |      color: #333333 !important;
      |      border-color: #bcd9f5 !important;
      |    }
      |
      |    /* ==========================================
      |       Computed cells — inert, not focusable
      |       ========================================== */
      |    .calc-cell-wrap,
      |    .calc-cell {
      |      pointer-events: none;
      |      user-select: none;
      |      -webkit-user-select: none;
      |      cursor: default;
      |      outline: none;
      |    }
      |    .calc-cell-wrap:focus,
      |    .calc-cell:focus,
      |    .calc-cell-wrap:focus-within {
      |      outline: none;
      |      box-shadow: none;
      |    }
      |    .calc-cell {
      |      display: block;
      |      height: var(--grid-input-height);
      |      line-height: var(--grid-input-height);
      |      padding: 0 6px;
      |      background: #f5f5f5;
      |      color: #333;
      |      border: 1px solid #e5e7eb;
      |      border-radius: 4px;
      |      font-variant-numeric: tabular-nums;
      |      text-align: right;
      |    }
      |
      |    /* ==========================================
      |       Custom combobox (if used)
      |       ========================================== */
      |    .combo { position: relative; width: 100%; }
      |    .combo-trigger {
      |      width: 100%; text-align: left;
      |      padding: 6px 10px; font-size: 14px;
      |      border: 1px solid #ccc; border-radius: 4px;
      |      background: white; cursor: pointer;
      |      display: flex; justify-content: space-between; align-items: center;
      |      font-family: inherit;
      |    }
      |    .combo-trigger:hover { border-color: #999; }
      |    .combo-arrow { color: #888; font-size: 12px; }
      |    .combo-list {
      |      position: absolute; top: 100%; left: 0; right: 0;
      |      max-height: 260px; overflow-y: auto;
      |      margin: 4px 0 0; padding: 0; list-style: none;
      |      background: white; border: 1px solid #ccc; border-radius: 4px;
      |      box-shadow: 0 4px 12px rgba(0,0,0,0.1);
      |      z-index: 100;
      |    }
      |    .combo-item {
      |      padding: 5px 10px; cursor: pointer;
      |      font-size: 14px; white-space: nowrap;
      |      overflow: hidden; text-overflow: ellipsis;
      |    }
      |    .combo-item.opt-odd  { background-color: #ffffff; }
      |    .combo-item.opt-even { background-color: #eef4fb; }
      |    .combo-item:hover    { background-color: #e3f2fd; }
      |    .combo-item.opt-selected { font-weight: 600; color: #1976d2; }
      |
      |    /* ==========================================
      |       Collapsible form sections (<details>)
      |       ========================================== */
      |    .htmx-form {
      |      display: block;
      |    }
      |
      |    .collapsible-section {
      |      border: 1px solid #e5e7eb;
      |      border-radius: 6px;
      |      background: #fff;
      |      margin-bottom: var(--grid-row-gap);
      |      overflow: hidden;
      |    }
      |    .collapsible-section + .collapsible-section {
      |      margin-top: 2px;
      |    }
      |
      |    .collapsible-summary {
      |      display: flex;
      |      align-items: center;
      |      gap: 6px;
      |      cursor: pointer;
      |      user-select: none;
      |      padding: 4px 10px;
      |      font-size: var(--card-title-size);
      |      font-weight: 600;
      |      line-height: 1.3;
      |      background: #f7f8fa;
      |      border-bottom: 1px solid transparent;
      |      list-style: none;
      |    }
      |    .collapsible-summary::-webkit-details-marker { display: none; }
      |    .collapsible-section[open] > .collapsible-summary {
      |      border-bottom-color: #e5e7eb;
      |    }
      |    .collapsible-summary:hover { background: #eef4fb; }
      |
      |    .collapsible-arrow {
      |      display: inline-block;
      |      width: 10px;
      |      font-size: 12px;
      |      color: #666;
      |      transition: transform 0.12s ease;
      |    }
      |    .collapsible-arrow::before { content: "▸"; }
      |    .collapsible-section[open] > .collapsible-summary .collapsible-arrow::before {
      |      content: "▾";
      |    }
      |
      |    .collapsible-content {
      |      display: grid;
      |      grid-template-columns: max-content 1fr max-content 1fr;
      |      column-gap: var(--grid-col-gap);
      |      row-gap:    var(--grid-row-gap);
      |      align-items: center;
      |      background: #fafafa;
      |      padding: 8px;
      |      border-radius: 0;
      |    }
      |    .collapsible-content > input[type="hidden"] { display: none; }
      |    .collapsible-content > .label {
      |      white-space: nowrap;
      |      justify-self: start;
      |      padding: 0;
      |      font-size: var(--grid-label-size);
      |      line-height: 1.2;
      |    }
      |    .collapsible-content > textarea {
      |      min-height: var(--grid-textarea-min);
      |      resize: vertical;
      |    }
      |
      |    .collapsible-content > .input,
      |    .collapsible-content > .select,
      |    .collapsible-content > .textarea {
      |      height: var(--grid-input-height);
      |      padding: 2px 6px;
      |      font-size: var(--grid-label-size);
      |    }
      |    .collapsible-content > .textarea { height: auto; }
      |    .collapsible-content > .btn {
      |      height: var(--grid-input-height);
      |      min-height: var(--grid-input-height);
      |      padding: var(--btn-pad);
      |      font-size: var(--grid-label-size);
      |    }
      |    .collapsible-content > .checkbox { height: 16px; width: 16px; }
      |
      |    .form-readonly .collapsible-summary { pointer-events: auto; }
      |
      |    /* ==========================================
      |       Partner form — light blue tint
      |       ========================================== */
      |    .partner-form .collapsible-section {
      |      border-color: #bcd9f5;
      |    }
      |    .partner-form .collapsible-summary {
      |      background: #d6e9fb;
      |    }
      |    .partner-form .collapsible-summary:hover {
      |      background: #c6e0f8;
      |    }
      |    .partner-form .collapsible-content {
      |      background: #eaf4fe;
      |    }
      |
      |    /* ==========================================
      |       Article form — light green tint
      |       ========================================== */
      |    .article-form .collapsible-section {
      |      border-color: #c3e6cb;
      |    }
      |    .article-form .collapsible-summary {
      |      background: #d4edda;
      |    }
      |    .article-form .collapsible-summary:hover {
      |      background: #c3e6cb;
      |    }
      |    .article-form .collapsible-content {
      |      background: #eaf7ee;
      |    }
      |
      |    /* ==========================================
      |       Transaction form — soft lavender tint
      |       ========================================== */
      |    .transaction-form .collapsible-section {
      |      border-color: #cdb8ec;
      |      box-shadow: 0 1px 2px rgba(90, 50, 140, 0.05);
      |    }
      |    .transaction-form .collapsible-summary {
      |      background: linear-gradient(135deg, #ece2fa 0%, #e0d0f7 100%);
      |      color: #4a2b7a;
      |    }
      |    .transaction-form .collapsible-summary:hover {
      |      background: linear-gradient(135deg, #e0d0f7 0%, #d4bef4 100%);
      |    }
      |    .transaction-form .collapsible-content {
      |      background: #f6f1fd;
      |    }
      |
      |    .transaction-form .htmx-subgrid input[type="number"] {
      |      text-align: right;
      |      font-variant-numeric: tabular-nums;
      |      -moz-appearance: textfield;
      |    }
      |    .transaction-form .htmx-subgrid input[type="number"]::-webkit-outer-spin-button,
      |    .transaction-form .htmx-subgrid input[type="number"]::-webkit-inner-spin-button {
      |      -webkit-appearance: none;
      |      margin: 0;
      |    }
      |    .transaction-form .htmx-subgrid input[type="date"] {
      |      font-size: var(--grid-label-size);
      |      height: var(--grid-input-height);
      |    }
      |
      |    /* ==========================================
      |       Financials form — warm amber tint
      |       ========================================== */
      |    .financials-form .collapsible-section {
      |      border-color: #f0d3a0;
      |      box-shadow: 0 1px 2px rgba(140, 100, 0, 0.05);
      |    }
      |    .financials-form .collapsible-summary {
      |      background: linear-gradient(135deg, #fbe7c6 0%, #f6d9a8 100%);
      |      color: #6a4a10;
      |    }
      |    .financials-form .collapsible-summary:hover {
      |      background: linear-gradient(135deg, #f6d9a8 0%, #f0c88a 100%);
      |    }
      |    .financials-form .collapsible-content {
      |      background: #fef8ec;
      |    }
      |
      |    /* ==========================================
      |       Sub-grid total — badge colour per form
      |       ========================================== */
      |    .transaction-form .subgrid-total .calc-total {
      |      background: #ece2fa;
      |      border-color: #cdb8ec;
      |    }
      |    .financials-form .subgrid-total .calc-total {
      |      background: #fbe7c6;
      |      border-color: #f0d3a0;
      |    }
      |""".stripMargin