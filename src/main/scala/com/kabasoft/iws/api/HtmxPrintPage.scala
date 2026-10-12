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
       |    h1 { font-size: 16pt; margin: 0 0 4mm 0; }
       |    h2 { font-size: 12pt; margin: 6mm 0 2mm 0; }
       |
       |    /* ---------- form widgets rendered as static text ---------- */
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
       |    /* ---------- collapsible sections: open, no arrows ---------- */
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
       |    /* The first section title acts as the document title. */
       |    details.collapsible-section:first-of-type > summary {
       |      font-size: 14pt;
       |      font-weight: 700;
       |      border-bottom: 2px solid #333;
       |      padding: 2mm 0;
       |      margin-bottom: 3mm;
       |    }
       |
       |    /* The 4-column label/value grid, kept for the printed page. */
       |    details.collapsible-section > .collapsible-content,
       |    .collapsible-content {
       |      display: grid !important;
       |      grid-template-columns: max-content 1fr max-content 1fr;
       |      column-gap: 6mm;
       |      row-gap: 2mm;
       |      align-items: center;
       |      background: transparent !important;
       |      padding: 2mm 0;
       |    }
       |    .collapsible-content > .label {
       |      white-space: nowrap;
       |      justify-self: start;
       |      color: var(--muted);
       |      font-size: 10pt;
       |      line-height: 1.2;
       |    }
       |    .collapsible-content > input[type="hidden"] { display: none !important; }
       |    .collapsible-content > .subgrid-block,
       |    .collapsible-content > .form-subgrid {
       |      grid-column: 1 / -1;
       |    }
       |
       |    /* ---------- subgrid: bordered table, actions hidden ---------- */
       |    .htmx-subgrid { width: 100%; margin-top: 2mm; }
       |    .htmx-subgrid table { width: 100%; border-collapse: collapse; font-size: 10pt; }
       |    .htmx-subgrid th,
       |    .htmx-subgrid td {
       |      border-bottom: 1px solid var(--line);
       |      padding: 1.5mm 2mm;
       |      text-align: left;
       |    }
       |    .htmx-subgrid th { font-weight: 700; background: #f4f4f4; }
       |    .htmx-subgrid .num { text-align: right; font-variant-numeric: tabular-nums; }
       |    .htmx-subgrid .subgrid-actions { display: none !important; }
       |    .htmx-subgrid > button { display: none !important; }
       |    .htmx-subgrid .calc-cell {
       |      display: inline !important;
       |      border: none !important; background: transparent !important;
       |      padding: 0 !important;
       |      font-variant-numeric: tabular-nums;
       |      text-align: right;
       |    }
       |    .htmx-subgrid .fk-cell { background: transparent !important; color: #222 !important; }
       |    .htmx-subgrid input.input-xs,
       |    .htmx-subgrid select.select-xs {
       |      height: auto !important;
       |      padding: 0 !important;
       |    }
       |    .subgrid-block { margin-top: 2mm; }
       |
       |    /* ---------- totals ---------- */
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
       |    /* ---------- never print these ---------- */
       |    .htmx-toolbar,
       |    .card-header-row,
       |    .table-scroll,
       |    .table-pager,
       |    .table-toolbar,
       |    .toggle-arrow,
       |    .toolbar-sep,
       |    .htmx-subgrid .subgrid-actions { display: none !important; }
       |
       |    /* ---------- page setup ---------- */
       |    @page { size: A4; margin: 12mm 15mm; }
       |    @media print {
       |      body { padding: 0; }
       |      details.collapsible-section { break-inside: avoid; }
       |      .htmx-subgrid tr { break-inside: avoid; }
       |    }
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