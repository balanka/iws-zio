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
      |       Form grid
      |       ========================================== */
      |    .htmx-grid {
      |      display: grid;
      |      grid-template-columns: max-content 1fr max-content 1fr;
      |      column-gap: var(--grid-col-gap);
      |      row-gap:    var(--grid-row-gap);
      |      align-items: center;
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
      |    /* Input with a currency suffix badge */
      |    .input-money { display: flex; align-items: center; gap: 4px; width: 100%; }
      |    .input-money > .input { flex: 1; text-align: right; }
      |    .input-money > .currency-suffix {
      |      font-size: var(--grid-label-size);
      |      color: #666;
      |      padding: 0 2px;
      |      white-space: nowrap;
      |      min-width: 24px;
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
      |    .card.collapsed .card-content { display: none; }
      |    .card.collapsed .card-header-row { margin-bottom: 0; }
      |    .toggle-arrow { font-size: 12px; color: #666; display: inline-block; width: 10px; }
      |
      |    .card-body.compact { padding: var(--card-pad); }
      |
      |    /* ==========================================
      |       Table
      |       ========================================== */
      |    .htmx-table-wrapper { width: 100%; }
      |    .table-toolbar { margin-bottom: 4px; }
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
      |    .table-row { cursor: pointer; }
      |    .table-row:hover { background-color: #e3f2fd !important; }
      |    .table-sort { color: inherit; text-decoration: none; }
      |    .table-sort:hover { color: #1976d2; }
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
      |        /* ==========================================
      |       Read-only form mode (toggled by the Edit button)
      |       ========================================== */
      |    .form-readonly input:not([type="hidden"]),
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
      |
      |    /* Toolbar separator */
      |    .toolbar-sep {
      |      display: inline-block;
      |      width: 1px;
      |      height: 18px;
      |      background: #ddd;
      |      margin: 0 4px;
      |    }
      |""".stripMargin