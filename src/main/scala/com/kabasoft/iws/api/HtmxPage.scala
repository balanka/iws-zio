package com.kabasoft.iws.api

object HtmxPage:

  // Runs before the page renders — no flash of wrong density.
  private val densityBootstrap: String =
    """
      |<script>
      |  (function () {
      |     var d = 'compact';
      |     try { d = localStorage.getItem('iws-density') || 'compact'; } catch (e) {}
      |     if (d !== 'xdense' && d !== 'compact' && d !== 'normal' && d !== 'spacious') d = 'compact';
      |     document.documentElement.setAttribute('data-density', d);
      |  })();
      |</script>
      |""".stripMargin

  private val pageScript: String =
    """
      |<script>
      |  /* =====================================================================
      |     Shared event helpers
      |     HTMX 2.x does not guarantee that e.detail.target is a DOM element.
      |     ===================================================================== */
      |  (function () {
      |    window.iwsEventEl = function (e) {
      |      var d = e && e.detail;
      |      if (!d) return null;
      |      var el = d.elt || d.target;
      |      return (el && el.nodeType === 1) ? el : null;
      |    };
      |    window.iwsEventElId = function (e) {
      |      var el = window.iwsEventEl(e);
      |      return (el && typeof el.id === 'string') ? el.id : '';
      |    };
      |  })();
      |
      |  /* =====================================================================
      |     Multi-column sort
      |     ===================================================================== */
      |  (function () {
      |    function parseSpec(s) {
      |      if (!s) return [];
      |      return s.split(',').filter(Boolean).map(function (part) {
      |        var i = part.indexOf(':');
      |        if (i < 0) return [part, 'asc'];
      |        return [part.substring(0, i), part.substring(i + 1)];
      |      });
      |    }
      |    function encodeSpec(spec) {
      |      return spec.map(function (s) { return s[0] + ':' + s[1]; }).join(',');
      |    }
      |    document.body.addEventListener('click', function (e) {
      |      var link = e.target.closest('.table-sort');
      |      if (!link) return;
      |      var wrapper = link.closest('.htmx-table-wrapper');
      |      var hidden = wrapper && wrapper.querySelector('input[name="sort"]');
      |      var key = link.dataset.sortKey;
      |      if (!key || !hidden) return;
      |      var spec = parseSpec(hidden.value);
      |      var idx = spec.findIndex(function (s) { return s[0] === key; });
      |      var shift = e.shiftKey;
      |      if (!shift) {
      |        if (spec.length === 1 && idx === 0) {
      |          spec = [[key, spec[0][1] === 'asc' ? 'desc' : 'asc']];
      |        } else {
      |          spec = [[key, 'asc']];
      |        }
      |      } else {
      |        if (idx >= 0) {
      |          spec[idx] = [key, spec[idx][1] === 'asc' ? 'desc' : 'asc'];
      |        } else {
      |          spec.push([key, 'asc']);
      |        }
      |      }
      |      hidden.value = encodeSpec(spec);
      |    }, true);
      |  })();
      |
      |  /* =====================================================================
      |     Collapsible cards (form card / list card)
      |     ===================================================================== */
      |  (function () {
      |    function refresh(btn, collapsed) {
      |      var arrow = btn.querySelector('.toggle-arrow');
      |      if (arrow) arrow.textContent = collapsed ? '▸' : '▾';
      |      var label = btn.querySelector('.toggle-label');
      |      if (label && btn.dataset.labelShow && btn.dataset.labelHide) {
      |        label.textContent = collapsed ? btn.dataset.labelShow : btn.dataset.labelHide;
      |      }
      |    }
      |    document.body.addEventListener('click', function (e) {
      |      var btn = e.target.closest('[data-toggle]');
      |      if (!btn) return;
      |      e.preventDefault();
      |      var card = document.querySelector(btn.dataset.toggle);
      |      if (!card) return;
      |      card.classList.toggle('collapsed');
      |      refresh(btn, card.classList.contains('collapsed'));
      |    });
      |  })();
      |
      |  /* =====================================================================
      |     Show / hide the table search field
      |     ===================================================================== */
      |  (function () {
      |    function refresh() {
      |      var btn = document.getElementById('toggle-search-btn');
      |      if (!btn) return;
      |      var hidden = document.body.classList.contains('hide-search');
      |      var arrow = btn.querySelector('.toggle-arrow');
      |      if (arrow) arrow.textContent = hidden ? '▸' : '▾';
      |      var label = btn.querySelector('.toggle-label');
      |      if (label) label.textContent = hidden ? 'Show Search' : 'Hide Search';
      |    }
      |    document.body.addEventListener('click', function (e) {
      |      if (!e.target.closest('#toggle-search-btn')) return;
      |      e.preventDefault();
      |      document.body.classList.toggle('hide-search');
      |      refresh();
      |    });
      |    document.body.addEventListener('htmx:afterSwap', refresh);
      |    refresh();
      |  })();
      |
      |  /* =====================================================================
      |     Density selector
      |     ===================================================================== */
      |  (function () {
      |    function syncSelect() {
      |      var sel = document.getElementById('density-select');
      |      if (!sel) return;
      |      sel.value = document.documentElement.getAttribute('data-density') || 'compact';
      |    }
      |    document.body.addEventListener('change', function (e) {
      |      var d = e.target.value;
      |      if (d !== 'xdense' && d !== 'compact' && d !== 'normal' && d !== 'spacious') return;
      |      document.documentElement.setAttribute('data-density', d);
      |      try { localStorage.setItem('iws-density', d); } catch (err) {}
      |    });
      |    document.body.addEventListener('htmx:afterSwap', syncSelect);
      |    syncSelect();
      |  })();
      |
      |  /* =====================================================================
      |     Form edit-mode toggle
      |     ===================================================================== */
      |  (function () {
      |    function resolveForm(selector) {
      |      var el = document.querySelector(selector);
      |      if (!el) return null;
      |      if (el.tagName === 'FORM') return el;
      |      return el.querySelector('form') || el;
      |    }
      |
      |    function syncLabel() {
      |      var btn = document.getElementById('toggle-edit-btn');
      |      if (!btn) return;
      |      var form = resolveForm(btn.dataset.editTarget);
      |      if (!form) return;
      |      var locked = form.classList.contains('form-readonly');
      |      var label = btn.querySelector('.edit-label');
      |      if (label) label.textContent = locked ? 'Edit' : 'Cancel';
      |    }
      |
      |    document.body.addEventListener('click', function (e) {
      |      var btn = e.target.closest('#toggle-edit-btn');
      |      if (!btn) return;
      |      e.preventDefault();
      |      var form = resolveForm(btn.dataset.editTarget);
      |      if (!form) return;
      |      form.classList.toggle('form-readonly');
      |      if (!form.classList.contains('form-readonly')) {
      |        var first = form.querySelector('details.collapsible-section');
      |        if (first) first.open = true;
      |      }
      |      syncLabel();
      |    });
      |
      |    document.body.addEventListener('htmx:afterSwap', function (e) {
      |      var id = window.iwsEventElId(e);
      |      if (id && id.endsWith('-form')) syncLabel();
      |    });
      |    syncLabel();
      |  })();
      |
      |  /* =====================================================================
      |     Sub-grid add / remove
      |
      |     - addSubRow: appends a fresh row from the <template>.
      |     - removeSubRow: does NOT remove the row from the DOM. Instead it
      |       marks the row so its fields are still submitted on Save:
      |         * hidden transid  → -2 (backend delete convention)
      |         * the <tr> is hidden from view
      |       The backend sees the row with transid=-2 and deletes it from
      |       the database inside transactModifyInternal.
      |     ===================================================================== */
      |  (function () {
      |    var nextIdx = {};
      |
      |    window.addSubRow = function (gridId, prefix) {
      |      var tbody = document.getElementById(gridId + '-body');
      |      var template = document.getElementById(gridId + '-template');
      |      if (!tbody || !template) return;
      |
      |      var idx = nextIdx[prefix] = (nextIdx[prefix] === undefined)
      |        ? tbody.querySelectorAll('tr').length
      |        : nextIdx[prefix] + 1;
      |
      |      var html = template.innerHTML.replace(/__INDEX__/g, String(idx));
      |      tbody.insertAdjacentHTML('beforeend', html);
      |
      |      var grid = tbody.closest('.htmx-subgrid');
      |      if (window.iwsRecomputeGrid) window.iwsRecomputeGrid(grid);
      |    };
      |
      |    window.removeSubRow = function (btn) {
      |      var tr = btn.closest('tr');
      |      if (!tr) return;
      |      var grid = tr.closest('.htmx-subgrid');
      |
      |      // Mark row as deleted (backend convention: transid = -2).
      |      tr.querySelectorAll('[name$=".transid"]').forEach(function (el) {
      |        el.value = '-2';
      |      });
      |
      |      // Keep the row in the DOM so its fields are still submitted,
      |      // but hide it from the user.
      |      tr.style.display = 'none';
      |      tr.classList.add('row-deleted');
      |
      |      if (window.iwsRecomputeGrid && grid) window.iwsRecomputeGrid(grid);
      |    };
      |  })();
      |
      |  /* =====================================================================
      |     Sub-grid foreign keys
      |     ===================================================================== */
      |  (function () {
      |    function syncGrid(grid, newId) {
      |      var fk = grid.dataset.fkField;
      |      if (!fk) return;
      |      grid.querySelectorAll('input[name$=".' + fk + '"]').forEach(function (el) {
      |        el.value = newId;
      |      });
      |    }
      |
      |    document.body.addEventListener('input', function (e) {
      |      var el = e.target;
      |      if (!el || el.name !== 'id_display') return;
      |      var form = el.closest('form');
      |      if (!form) return;
      |      var newId = el.value;
      |      form.querySelectorAll('.htmx-subgrid').forEach(function (grid) {
      |        syncGrid(grid, newId);
      |      });
      |    });
      |  })();
      |
      |  /* =====================================================================
      |     Sub-grid: article-driven fill + computed cells + running total
      |
      |     - Picking an article fills Name (locked), Unit (locked), and the
      |       starting Price on that row.
      |     - Picking a VAT code (or changing Qty/Price) recomputes the VAT
      |       amount column using the option's data-rate attribute.
      |     - Adding or removing a row updates the running total.
      |     - Rows marked as deleted (transid = -2, .row-deleted) are skipped
      |       by every recompute so they don't affect Amount / VAT / Total.
      |
      |     Server-side applyParams recomputes vat authoritatively on save.
      |     ===================================================================== */
      |  (function () {
      |    function isDeleted(tr) {
      |      if (!tr) return false;
      |      if (tr.classList.contains('row-deleted')) return true;
      |      if (tr.style.display === 'none') return true;
      |      var t = tr.querySelector('[name$=".transid"]');
      |      return !!(t && t.value === '-2');
      |    }
      |
      |    function fieldValue(row, name) {
      |      // vatRate is not an input — it comes from the selected option
      |      // of the row's vatCode <select>.
      |      if (name === 'vatRate') {
      |        var sel = row.querySelector('select[name$=".vatCode"]');
      |        if (!sel) return 0;
      |        var opt = sel.options[sel.selectedIndex];
      |        if (!opt) return 0;
      |        var r = parseFloat(opt.dataset.rate);
      |        return isNaN(r) ? 0 : r;
      |      }
      |      var el = row.querySelector('[name$=".' + name + '"]');
      |      if (!el) return 0;
      |      var v = parseFloat(el.value);
      |      return isNaN(v) ? 0 : v;
      |    }
      |
      |    function recomputeRow(row) {
      |      if (isDeleted(row)) return;
      |      row.querySelectorAll('.calc-cell[data-expr]').forEach(function (span) {
      |        var expr = span.dataset.expr;
      |        var exprSub = expr.replace(/[a-zA-Z_][a-zA-Z0-9_]*/g, function (name) {
      |          if (['min', 'max', 'abs', 'round'].indexOf(name) >= 0) return name;
      |          return String(fieldValue(row, name));
      |        });
      |        var result;
      |        try { result = Function('"use strict";return (' + exprSub + ')')(); }
      |        catch (e) { result = null; }
      |        var n = parseFloat(result);
      |        if (isNaN(n)) n = 0;
      |        span.textContent = n.toFixed(2);
      |        var hidden = span.parentElement.querySelector('input[type="hidden"][data-calc]');
      |        if (hidden) hidden.value = n.toFixed(2);
      |      });
      |    }
      |
      |    function recomputeTotal(grid) {
      |      var field = grid.dataset.totalField;
      |      if (!field) return;
      |      var form = grid.closest('form');
      |      if (!form) return;
      |      var total = 0;
      |      grid.querySelectorAll('tbody tr').forEach(function (tr) {
      |        if (isDeleted(tr)) return;
      |        var el = tr.querySelector('[name$=".' + field + '"]');
      |        if (!el) return;
      |        var v = parseFloat(el.value);
      |        if (!isNaN(v)) total += v;
      |      });
      |      var out = form.querySelector('.calc-total');
      |      if (out) out.textContent = total.toFixed(2);
      |    }
      |
      |    function recomputeAll(root) {
      |      (root || document).querySelectorAll('.htmx-subgrid').forEach(function (grid) {
      |        grid.querySelectorAll('tbody tr').forEach(function (tr) {
      |          if (!isDeleted(tr)) recomputeRow(tr);
      |        });
      |        recomputeTotal(grid);
      |      });
      |    }
      |
      |    // Expose for use by addSubRow / removeSubRow
      |    window.iwsRecomputeGrid = function (grid) {
      |      if (!grid) return;
      |      grid.querySelectorAll('tbody tr').forEach(function (tr) {
      |        if (!isDeleted(tr)) recomputeRow(tr);
      |      });
      |      recomputeTotal(grid);
      |    };
      |
      |    // Input: recompute on every keystroke
      |    document.body.addEventListener('input', function (e) {
      |      var grid = e.target.closest('.htmx-subgrid');
      |      if (!grid) return;
      |      var row = e.target.closest('tr');
      |      if (row) recomputeRow(row);
      |      recomputeTotal(grid);
      |    });
      |
      |    // Change: article-driven fill, then recompute
      |    document.body.addEventListener('change', function (e) {
      |      var grid = e.target.closest('.htmx-subgrid');
      |      if (!grid) return;
      |      var row = e.target.closest('tr');
      |      if (!row) return;
      |
      |      // Article selected → fill Name, Unit, Price, and (optionally) VAT code
      |      if (e.target.matches('select[name$=".article"]')) {
      |        var opt = e.target.options[e.target.selectedIndex];
      |        if (opt) {
      |          var nameEl  = row.querySelector('[name$=".articleName"]');
      |          var unitEl  = row.querySelector('[name$=".unit"]');
      |          var priceEl = row.querySelector('[name$=".price"]');
      |          var vatEl   = row.querySelector('select[name$=".vatCode"]');
      |
      |          if (nameEl)  nameEl.value  = opt.dataset.name  || '';
      |          if (unitEl)  unitEl.value  = opt.dataset.unit  || '';
      |          if (priceEl) priceEl.value = opt.dataset.price || '0';
      |          if (vatEl && opt.dataset.vatcode) {
      |            vatEl.value = opt.dataset.vatcode;
      |          }
      |        }
      |      }
      |
      |      recomputeRow(row);
      |      recomputeTotal(grid);
      |    });
      |
      |    document.body.addEventListener('htmx:afterSwap', function (e) {
      |      var el = window.iwsEventEl(e);
      |      if (el) recomputeAll(el);
      |    });
      |
      |    // Initial pass
      |    recomputeAll(document);
      |  })();
      |
      |  /* =====================================================================
      |     Collapsible sections (<details>)
      |     ===================================================================== */
      |  (function () {
      |    var KEY = 'iws-collapse:';
      |
      |    function persist(d) {
      |      if (!d.id) return;
      |      try { localStorage.setItem(KEY + d.id, d.open ? '1' : '0'); } catch (e) {}
      |    }
      |    function restore(root) {
      |      (root || document)
      |        .querySelectorAll('details.collapsible-section')
      |        .forEach(function (d) {
      |          if (!d.id) return;
      |          var saved = null;
      |          try { saved = localStorage.getItem(KEY + d.id); } catch (e) {}
      |          if (saved !== null) d.open = saved === '1';
      |        });
      |    }
      |    function openInvalid(root) {
      |      (root || document)
      |        .querySelectorAll('input:invalid, select:invalid, textarea:invalid')
      |        .forEach(function (el) {
      |          var sec = el.closest('details.collapsible-section');
      |          if (sec) sec.open = true;
      |        });
      |    }
      |
      |    document.body.addEventListener('toggle', function (e) {
      |      if (e.target instanceof HTMLDetailsElement) persist(e.target);
      |    }, true);
      |
      |    document.body.addEventListener('htmx:afterSwap', function (e) {
      |      var el = window.iwsEventEl(e);
      |      if (!el) return;
      |      restore(el);
      |    });
      |
      |    document.body.addEventListener('htmx:afterRequest', function (e) {
      |      if (!e.detail || !e.detail.successful) return;
      |      var el = window.iwsEventEl(e);
      |      if (!el) return;
      |      openInvalid(el);
      |    });
      |
      |    restore(document);
      |  })();
      |
      |  /* =====================================================================
      |     Filter selects — never disabled (JS safety net)
      |     ===================================================================== */
      |  (function () {
      |    function unlock(root) {
      |      (root || document)
      |        .querySelectorAll('select.filter-select')
      |        .forEach(function (el) {
      |          if (el.disabled) el.disabled = false;
      |          el.removeAttribute('disabled');
      |          el.style.pointerEvents = 'auto';
      |          el.style.cursor = 'pointer';
      |          el.style.opacity = '1';
      |        });
      |    }
      |
      |    document.body.addEventListener('htmx:afterSwap', function (e) {
      |      var el = window.iwsEventEl(e);
      |      if (el) unlock(el);
      |    });
      |    document.body.addEventListener('htmx:afterSettle', function (e) {
      |      var el = window.iwsEventEl(e);
      |      if (el) unlock(el);
      |    });
      |
      |    unlock(document);
      |  })();
      |</script>
      |""".stripMargin

  def render(title: String, formHtml: String): String =
    s"""<!doctype html>
       |<html lang="en" data-theme="light">
       |<head>
       |  <meta charset="utf-8" />
       |  <title>${Html.esc(title)}</title>
       |  <link href="https://cdn.jsdelivr.net/npm/daisyui@5" rel="stylesheet" type="text/css" />
       |  <script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
       |  <script src="https://unpkg.com/htmx.org@2.0.4"></script>
       |  $densityBootstrap
       |  <style>
       |${HtmxCss.grid}  </style>
       |</head>
       |<body class="bg-base-200 hide-search">
       |  <div class="container mx-auto max-w-3xl">
       |    <div class="htmx-page-wrapper">
       |      <div class="card bg-base-100 shadow-sm">
       |        <div class="card-body compact">
       |          <h2 class="card-title mb-4">${Html.esc(title)}</h2>
       |          $formHtml
       |        </div>
       |      </div>
       |    </div>
       |  </div>
       |$pageScript
       |</body>
       |</html>""".stripMargin

  def renderRaw(title: String, bodyHtml: String): String =
    s"""<!doctype html>
       |<html lang="en" data-theme="light">
       |<head>
       |  <meta charset="utf-8" />
       |  <title>${Html.esc(title)}</title>
       |  <link href="https://cdn.jsdelivr.net/npm/daisyui@5" rel="stylesheet" type="text/css" />
       |  <script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
       |  <script src="https://unpkg.com/htmx.org@2.0.4"></script>
       |  $densityBootstrap
       |  <style>
       |${HtmxCss.grid}  </style>
       |</head>
       |<body class="bg-base-200 hide-search">
       |$bodyHtml
       |$pageScript
       |</body>
       |</html>""".stripMargin