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
      |    };
      |
      |    window.removeSubRow = function (btn) {
      |      var tr = btn.closest('tr');
      |      if (tr) tr.remove();
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
      |     Sub-grid computed cells + running total
      |     ===================================================================== */
      |  (function () {
      |    function fieldValue(row, name) {
      |      var el = row.querySelector('[name$=".' + name + '"]');
      |      if (!el) return 0;
      |      var v = parseFloat(el.value);
      |      return isNaN(v) ? 0 : v;
      |    }
      |
      |    function recomputeRow(row) {
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
      |        grid.querySelectorAll('tbody tr').forEach(recomputeRow);
      |        recomputeTotal(grid);
      |      });
      |    }
      |
      |    document.body.addEventListener('input', function (e) {
      |      var grid = e.target.closest('.htmx-subgrid');
      |      if (!grid) return;
      |      var row = e.target.closest('tr');
      |      if (row) recomputeRow(row);
      |      recomputeTotal(grid);
      |    });
      |    document.body.addEventListener('change', function (e) {
      |      var grid = e.target.closest('.htmx-subgrid');
      |      if (!grid) return;
      |      var row = e.target.closest('tr');
      |      if (row) recomputeRow(row);
      |      recomputeTotal(grid);
      |    });
      |
      |    document.body.addEventListener('htmx:afterSwap', function (e) {
      |      var el = window.iwsEventEl(e);
      |      if (el) recomputeAll(el);
      |    });
      |
      |    recomputeAll(document);
      |  })();
      |
      |  /* =====================================================================
      |     Collapsible sections (<details>)
      |     - Persist open / closed per section id in localStorage.
      |     - Auto-open a section containing an invalid field after a
      |       failed htmx request.
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
      |     Removes the `disabled` attribute and re-enables pointer events
      |     on any select.filter-select after every HTMX swap / settle, and
      |     once on initial load. Guarantees the Transaction / fmodule
      |     selector stays usable in view mode regardless of CSS.
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