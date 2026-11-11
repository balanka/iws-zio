package com.kabasoft.iws.api

object HtmxPrintPage:

  def render(title: String, bodyHtml: String): String =
    s"""<!doctype html>
       |<html lang="en">
       |<head>
       |  <meta charset="utf-8" />
       |  <title>${Html.esc(title)}</title>
       |  <style>
       |    :root { --line: #d0d0d0; --muted: #666; }
       |    * { box-sizing: border-box; }
       |    html, body { margin: 0; padding: 0; }
       |    body {
       |      font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
       |      font-size: 11pt; color: #222; background: #fff;
       |      padding: 12mm 15mm;
       |    }
       |
       |    /* form widgets as static text */
       |    input:not([type="hidden"]):not([type="checkbox"]),
       |    textarea {
       |      border: none !important;
       |      background: transparent !important;
       |      padding: 0 !important;
       |      font: inherit; color: #222;
       |      width: 100% !important; height: auto !important;
       |      pointer-events: none;
       |    }
       |    input[type="hidden"] { display: none !important; }
       |    textarea { resize: none; min-height: 0 !important; }
       |
       |    select {
       |      border: none !important;
       |      background: transparent !important;
       |      background-image: none !important;
       |      padding: 0 !important;
       |      font: inherit; color: #222;
       |      width: 100% !important; height: auto !important;
       |      -webkit-appearance: none; -moz-appearance: none; appearance: none;
       |      pointer-events: none;
       |    }
       |    select option { color: #222; }
       |    input[type="checkbox"] {
       |      appearance: none; -webkit-appearance: none;
       |      width: 4mm; height: 4mm;
       |      border: 1px solid var(--line) !important;
       |      background: transparent !important;
       |      pointer-events: none;
       |      vertical-align: -0.5mm;
       |    }
       |    input[type="checkbox"]:checked::after {
       |      content: "✔"; display: block; text-align: center;
       |      font-size: 10pt; line-height: 4mm; color: #222;
       |    }
       |
       |    details.collapsible-section {
       |      border: none !important; margin: 0 0 3mm 0; overflow: visible;
       |    }
       |    details.collapsible-section > summary {
       |      list-style: none; padding: 1mm 0;
       |      background: transparent !important;
       |      border-bottom: 1px solid var(--line);
       |      font-weight: 700; font-size: 11.5pt;
       |    }
       |    details.collapsible-section > summary::-webkit-details-marker { display: none; }
       |    details.collapsible-section > summary .collapsible-arrow { display: none; }
       |
       |    details.collapsible-section:first-of-type > summary {
       |      font-size: 14pt;
       |      font-weight: 700;
       |      border-bottom: 2px solid #333;
       |      padding: 2mm 0;
       |      margin-bottom: 3mm;
       |    }
       |
       |    details.collapsible-section > .collapsible-content,
       |    .collapsible-content {
       |      display: grid !important;
       |      grid-template-columns: max-content 1fr max-content 1fr;
       |      column-gap: 6mm; row-gap: 2mm; align-items: center;
       |      background: transparent !important;
       |      padding: 2mm 0;
       |    }
       |    .collapsible-content > .label {
       |      white-space: nowrap; justify-self: start;
       |      color: var(--muted); font-size: 10pt; line-height: 1.2;
       |    }
       |    .collapsible-content > input[type="hidden"] { display: none !important; }
       |    .collapsible-content > .subgrid-block,
       |    .collapsible-content > .form-subgrid { grid-column: 1 / -1; }
       |
       |    .htmx-subgrid { width: 100%; margin-top: 2mm; }
       |    .htmx-subgrid table { width: 100%; border-collapse: collapse; font-size: 10pt; }
       |    .htmx-subgrid th,
       |    .htmx-subgrid td {
       |      border-bottom: 1px solid var(--line);
       |      padding: 1.5mm 2mm; text-align: left;
       |    }
       |    .htmx-subgrid th { font-weight: 700; background: #f4f4f4; }
       |    .htmx-subgrid .num { text-align: right; font-variant-numeric: tabular-nums; }
       |    .htmx-subgrid .subgrid-actions { display: none !important; }
       |    .htmx-subgrid > button { display: none !important; }
       |    .htmx-subgrid .calc-cell {
       |      display: inline !important;
       |      border: none !important; background: transparent !important;
       |      padding: 0 !important;
       |      font-variant-numeric: tabular-nums; text-align: right;
       |    }
       |    .htmx-subgrid .fk-cell { background: transparent !important; color: #222 !important; }
       |    .htmx-subgrid input.input-xs,
       |    .htmx-subgrid select.select-xs { height: auto !important; padding: 0 !important; }
       |    .subgrid-block { margin-top: 2mm; }
       |
       |    .subgrid-total {
       |      display: flex; justify-content: flex-end; align-items: center;
       |      gap: 3mm; margin-top: 2mm; font-weight: 700;
       |    }
       |    .subgrid-total .calc-total {
       |      border: none !important; background: transparent !important;
       |      padding: 0 !important; min-width: 0 !important;
       |      font-variant-numeric: tabular-nums;
       |    }
       |
       |    .htmx-toolbar, .card-header-row, .table-scroll, .table-pager,
       |    .table-toolbar, .toggle-arrow, .toolbar-sep,
       |    .htmx-subgrid .subgrid-actions,
       |    .col-resizer { display: none !important; }
       |
       |    /* ---- Sub-grid footer (tfoot) on paper ---- */
       |    .htmx-subgrid tfoot td {
       |      font-weight: 700;
       |      border-top: 1.5px solid var(--line);
       |      background: transparent !important;
       |      white-space: nowrap;
       |      text-align: right;
       |    }
       |    .htmx-subgrid tfoot .subgrid-foot-label {
       |      font-weight: 700;
       |      color: #222;
       |      margin-right: 2px;
       |      display: inline;
       |      white-space: nowrap;
       |    }
       |    .htmx-subgrid tfoot .calc-vat-total,
       |    .htmx-subgrid tfoot .calc-total,
       |    .htmx-subgrid tfoot .currency-suffix {
       |      display: inline;
       |      white-space: nowrap;
       |    }
       |    .htmx-subgrid tfoot .currency-suffix {
       |      margin-left: 3px;
       |      color: #444;
       |    }
       |
       |    /* On paper: hide the header Foot text; show the footer copy styled
       |       exactly like the header field (label left, box right). */
       |    label.print-foot-label,
       |    textarea.print-foot-label { display: none !important; }
       |
       |    /* ==========================================
       |       Printed sub-grid — redistribute the space
       |       freed by hiding Id and Due date.
       |       Only affects the printed page.
       |       ========================================== */
       |    /* Hide the two columns on paper. */
       |    .print-hide { display: none !important; }
       |
       |    /* Make the printed table use the full page width. */
       |    .htmx-subgrid table {
       |      table-layout: fixed;
       |      width: 100%;
       |    }
       |
       |    /* Widen the surviving columns in the ratio:
       |         article   50%
       |         vat amt   20%
       |         vat       20%
       |         quantity   5%
       |         price      5%
       |       Anything not listed keeps a share (text, unit, amount)
       |       via the leftover 1fr-equivalent computed by the browser. */
       |    .htmx-subgrid th.th-article,
       |    .htmx-subgrid td:nth-child(2) { width: 26%; }
       |
       |    .htmx-subgrid th.th-quantity,
       |    .htmx-subgrid td:nth-child(4) { width: 7%;  }
       |
       |    .htmx-subgrid th.th-unit,
       |    .htmx-subgrid td:nth-child(5) { width: 5%;  }
       |
       |    .htmx-subgrid th.th-price,
       |    .htmx-subgrid td:nth-child(6) { width: 8%;  }
       |
       |    .htmx-subgrid th.th-amount,
       |    .htmx-subgrid td:nth-child(7) { width: 10%;  }
       |
       |    .htmx-subgrid th.th-vatCode,
       |    .htmx-subgrid td:nth-child(8) { width: 8%;  }
       |
       |    .htmx-subgrid th.th-vat,
       |    .htmx-subgrid td:nth-child(9) { width: 12%; }
       |
       |    .htmx-subgrid th.th-text,
       |    .htmx-subgrid td:nth-child(11) { width: 24%; }
       |
       |    .print-foot-grid {
       |      display: grid !important;
       |      grid-template-columns: max-content 1fr;
       |      column-gap: 6mm;
       |      align-items: start;
       |      margin-top: 8mm;
       |      padding-top: 3mm;
       |      border-top: 1px solid var(--line);
       |      font-size: 11pt;
       |    }
       |    .print-foot-grid > .label {
       |      white-space: nowrap;
       |      color: var(--muted);
       |      font-size: 10pt;
       |      line-height: 1.2;
       |      padding-top: 1mm;
       |    }
       |    .print-foot-box {
       |      display: block;
       |      width: 100%;
       |      min-height: 6mm;
       |      padding: 1.5mm 2mm;
       |      border: 1px solid var(--line);
       |      border-radius: 4px;
       |      background: transparent;
       |      white-space: pre-wrap;
       |      overflow-wrap: anywhere;
       |      text-align: left;
       |      line-height: 1.4;
       |      color: #222;
       |    }
       |
       |    @page { size: A4; margin: 12mm 15mm; }
       |    @media print {
       |      body { padding: 0; }
       |      details.collapsible-section { break-inside: avoid; }
       |      .htmx-subgrid tr { break-inside: avoid; }
       |      .print-foot-grid { break-inside: avoid; }
       |    }
       |   /* Columns hidden on paper only (e.g. Id and Due date in the
       |       Transaction subgrid). Add `printHidden = true` on the column
       |       to mark it. */
       |    .print-hide { display: none !important; }
       |  </style>
       |</head>
       |<body>
       |$bodyHtml
       |
       |<script>
       |  window.addEventListener('load', function () {
       |    document.querySelectorAll('details.collapsible-section').forEach(function (d) {
       |      d.open = true;
       |    });
       |    window.print();
       |  });
       |  window.addEventListener('afterprint', function () {
       |    window.close();
       |  });
       |</script>
       |</body>
       |</html>""".stripMargin