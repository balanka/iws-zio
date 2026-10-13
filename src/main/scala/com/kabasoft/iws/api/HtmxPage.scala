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
      |     Sub-grid add / remove + dirty-flag
      |
      |     Server-side rules for lines:
      |       new line            → id = -1
      |       existing unchanged  → id > 0 and transid > 0
      |       existing modified   → id > 0 and transid = -1
      |       existing deleted    → id = -2
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
      |      // Mark row as deleted — server convention: id = -2
      |      var idEl = tr.querySelector('[name$=".id"]');
      |      if (idEl) idEl.value = '-2';
      |
      |      // Keep the row in the DOM (so its fields still submit), hide it.
      |      tr.style.display = 'none';
      |      tr.classList.add('row-deleted');
      |
      |      if (window.iwsRecomputeGrid && grid) window.iwsRecomputeGrid(grid);
      |    };
      |
      |    // Any edit to an existing row marks it dirty → transid = -1.
      |    // New rows (id = -1) and deleted rows (id = -2) are left alone.
      |    window.iwsMarkRowDirty = function (tr) {
      |      if (!tr) return;
      |      var idEl      = tr.querySelector('[name$=".id"]');
      |      var transidEl = tr.querySelector('[name$=".transid"]');
      |      if (!idEl || !transidEl) return;
      |      var idv = parseInt(idEl.value, 10);
      |      if (!isNaN(idv) && idv > 0) {
      |        transidEl.value = '-1';
      |      }
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
      |     - Picking an article fills Name (locked), Unit (locked) and Price.
      |     - Picking a VAT code (or changing Qty/Price) recomputes VAT.
      |     - Adding / removing a row updates the running total.
      |     - Any edit to an existing row sets transid = -1 (dirty marker).
      |     ===================================================================== */
      |  (function () {
      |    function isDeleted(tr) {
      |      if (!tr) return false;
      |      if (tr.classList.contains('row-deleted')) return true;
      |      if (tr.style.display === 'none') return true;
      |      var idEl = tr.querySelector('[name$=".id"]');
      |      return !!(idEl && idEl.value === '-2');
      |    }
      |
      |    function fieldValue(row, name) {
      |      // vatRate comes from the selected option of the row's vatCode <select>.
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
      |      var field = grid.dataset.totalField || 'amount';
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
      |    window.iwsRecomputeGrid = function (grid) {
      |      if (!grid) return;
      |      grid.querySelectorAll('tbody tr').forEach(function (tr) {
      |        if (!isDeleted(tr)) recomputeRow(tr);
      |      });
      |      recomputeTotal(grid);
      |    };
      |
      |    // Input: mark dirty, then recompute
      |    document.body.addEventListener('input', function (e) {
      |      var grid = e.target.closest('.htmx-subgrid');
      |      if (!grid) return;
      |      var row = e.target.closest('tr');
      |      if (row) {
      |        if (window.iwsMarkRowDirty) window.iwsMarkRowDirty(row);
      |        recomputeRow(row);
      |      }
      |      recomputeTotal(grid);
      |    });
      |
      |    // Change: article-driven fill, mark dirty, recompute
      |    document.body.addEventListener('change', function (e) {
      |      var grid = e.target.closest('.htmx-subgrid');
      |      if (!grid) return;
      |      var row = e.target.closest('tr');
      |      if (!row) return;
      |
      |      // Article selected → fill Name, Unit, Price, VAT code
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
      |      // Financials: account → accountName / oaccountName
      |      if (e.target.matches('select[name$=".account"]')) {
      |        var opt = e.target.options[e.target.selectedIndex];
      |        if (opt) {
      |          var nameEl = row.querySelector('[name$=".accountName"]');
      |          if (nameEl) nameEl.value = opt.dataset.name || '';
      |        }
      |      }
      |      if (e.target.matches('select[name$=".oaccount"]')) {
      |        var opt = e.target.options[e.target.selectedIndex];
      |        if (opt) {
      |          var nameEl = row.querySelector('[name$=".oaccountName"]');
      |          if (nameEl) nameEl.value = opt.dataset.name || '';
      |        }
      |      }
      |
      |      if (window.iwsMarkRowDirty) window.iwsMarkRowDirty(row);
      |      recomputeRow(row);
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
      |
      |  /* =====================================================================
      |     Card title follows the fmodule selector
      |     - When the fmodule combo (.filter-select inside a .htmx-form)
      |       changes, replace the card's .card-title with
      |       "<company>/<fmodule name>".
      |     - The company is read from the form's hidden [name='company'] input.
      |     - The combo's option label is "105 Purchase order"; we drop the
      |       leading id and show "Purchase order".
      |     ===================================================================== */
      |  (function () {
      |    function labelWithoutId(text) {
      |      var t = (text || '').trim();
      |      if (!t) return '';
      |      var parts = t.split(/\s+/);
      |      if (parts.length > 1) parts.shift();          // drop leading "105" / "TR-01"
      |      return parts.join(' ') || t;
      |    }
      |
      |    document.body.addEventListener('change', function (e) {
      |      var sel = e.target;
      |      if (!sel || !sel.matches) return;
      |      if (!sel.matches('select.filter-select')) return;
      |
      |      var form = sel.closest('form.htmx-form');
      |      if (!form) return;
      |
      |      var card = form.closest('.card');
      |      if (!card) return;
      |
      |      var h2 = card.querySelector('.card-title');
      |      if (!h2) return;
      |
      |      var opt = sel.options[sel.selectedIndex];
      |      if (!opt) return;
      |
      |      var name = labelWithoutId(opt.textContent);
      |      if (!name) return;
      |
      |      var companyEl = form.querySelector('[name="company"]');
      |      var company   = companyEl ? (companyEl.value || '').trim() : '';
      |
      |      h2.textContent = company ? (company + '/' + name) : name;
      |    });
      |  })();
      |
      |    /* =====================================================================
      |     Print the currently displayed record
      |     - Reads id / modelid / company from the form.
      |     - Opens a new browser tab at /html/<prefix>/print/<id>/<modelid>/<company>.
      |     - That page auto-triggers window.print() and closes when done.
      |     ===================================================================== */
      |  (function () {
      |    window.iwsPrintForm = function (prefix) {
      |      var form = document.getElementById(prefix + '-form');
      |      if (!form) return;
      |
      |      var val = function (name) {
      |        var el = form.querySelector('[name="' + name + '"]');
      |        return el ? (el.value || '').trim() : '';
      |      };
      |
      |      var id      = val('id_display') || val('id');
      |      var modelid = val('record_modelid') || val('modelid');
      |      var company = val('company');
      |
      |      if (!id || !modelid || !company) return;
      |
      |      var url = '/html/' + prefix + '/print/'
      |              + encodeURIComponent(id)      + '/'
      |              + encodeURIComponent(modelid) + '/'
      |              + encodeURIComponent(company);
      |
      |      window.open(url, '_blank');
      |    };
      |  })();
      |
      |    /* =====================================================================
      |     Add-line button in the toolbar
      |     - Finds the active subgrid inside the currently focused form
      |       (or the first subgrid on the page) and calls addSubRow on it.
      |     - Hides the button entirely when no subgrid is present.
      |     ===================================================================== */
      |  (function () {
      |    window.iwsAddLineToActiveGrid = function () {
      |      var activeForm =
      |        document.activeElement && document.activeElement.closest
      |          ? document.activeElement.closest('form')
      |          : null;
      |
      |      var grid =
      |        (activeForm && activeForm.querySelector('.htmx-subgrid')) ||
      |        document.querySelector('.htmx-subgrid');
      |
      |      if (!grid) return;
      |      var gridId = grid.dataset.gridId;
      |      var prefix = grid.dataset.prefix;
      |      if (!gridId || !prefix) return;
      |      if (window.addSubRow) window.addSubRow(gridId, prefix);
      |    };
      |
      |    function syncAddLineButton() {
      |      var btn = document.getElementById('add-line-btn');
      |      if (!btn) return;
      |      var hasGrid = !!document.querySelector('.htmx-subgrid');
      |      btn.style.display = hasGrid ? '' : 'none';
      |    }
      |
      |    document.body.addEventListener('htmx:afterSwap',  syncAddLineButton);
      |    document.body.addEventListener('htmx:afterSettle', syncAddLineButton);
      |    syncAddLineButton();
      |  })();
      |
      |    /* =====================================================================
      |     Resizable table columns
      |     - A `<span class="col-resizer" data-col-key="...">` in a <th> starts
      |       a drag. Moving the mouse updates the <th>'s inline width.
      |     - The width is stored in localStorage keyed by table path + column.
      |     - After every HTMX swap the saved widths are re-applied, so paging,
      |       sorting and filtering don't lose them.
      |     ===================================================================== */
      |  (function () {
      |    var PREFIX = 'iws-colwidth:';
      |    var dragState = null;
      |
      |    function storageKey(tableKey, colKey) {
      |      return PREFIX + tableKey + ':' + colKey;
      |    }
      |
      |    function applyWidths(root) {
      |      (root || document).querySelectorAll('table.htmx-table-fixed').forEach(function (table) {
      |        var tableKey = table.dataset.tableKey;
      |        if (!tableKey) return;
      |        table.querySelectorAll('th[data-col-key]').forEach(function (th) {
      |          var colKey = th.dataset.colKey;
      |          if (!colKey) return;
      |          var saved = null;
      |          try { saved = localStorage.getItem(storageKey(tableKey, colKey)); } catch (e) {}
      |          if (saved) th.style.width = saved;
      |        });
      |      });
      |    }
      |
      |    document.body.addEventListener('mousedown', function (e) {
      |      var handle = e.target.closest('.col-resizer');
      |      if (!handle) return;
      |      e.preventDefault();
      |      e.stopPropagation();
      |
      |      var th    = handle.closest('th');
      |      var table = th && th.closest('table.htmx-table-fixed');
      |      if (!th || !table) return;
      |
      |      dragState = {
      |        th:         th,
      |        table:      table,
      |        colKey:     handle.dataset.colKey,
      |        tableKey:   table.dataset.tableKey,
      |        startX:     e.clientX,
      |        startWidth: th.offsetWidth
      |      };
      |      document.body.classList.add('col-resizing');
      |    }, true);
      |
      |    document.body.addEventListener('mousemove', function (e) {
      |      if (!dragState) return;
      |      e.preventDefault();
      |      var delta    = e.clientX - dragState.startX;
      |      var newWidth = Math.max(40, dragState.startWidth + delta);
      |      dragState.th.style.width = newWidth + 'px';
      |    }, true);
      |
      |    document.body.addEventListener('mouseup', function (e) {
      |      if (!dragState) return;
      |      var w = dragState.th.style.width;
      |      if (dragState.tableKey && dragState.colKey && w) {
      |        try {
      |          localStorage.setItem(
      |            storageKey(dragState.tableKey, dragState.colKey), w);
      |        } catch (err) {}
      |      }
      |      dragState = null;
      |      document.body.classList.remove('col-resizing');
      |    }, true);
      |
      |    document.body.addEventListener('htmx:afterSwap', function (e) {
      |      var el = window.iwsEventEl(e);
      |      if (el) applyWidths(el);
      |    });
      |    document.body.addEventListener('htmx:afterSettle', function (e) {
      |      var el = window.iwsEventEl(e);
      |      if (el) applyWidths(el);
      |    });
      |
      |    applyWidths(document);
      |  })();
      |
      |    /* =====================================================================
      |     Period follows the header's Transaction date
      |     - Picking a new transdate updates the readonly Period input to
      |       YYYYMM before Save.
      |     ===================================================================== */
      |  (function () {
      |    function syncPeriod(form) {
      |      if (!form) return;
      |      var td = form.querySelector('input[name="transdate"]');
      |      var pd = form.querySelector('input[name="period"]');
      |      if (!td || !pd) return;
      |      var v = td.value;
      |      if (!v || v.length < 7) return;
      |      var parts = v.split('-');
      |      if (parts.length < 3) return;
      |      pd.value = parts[0] + parts[1];
      |    }
      |
      |    document.body.addEventListener('change', function (e) {
      |      var el = e.target;
      |      if (!el || !el.matches) return;
      |      if (!el.matches('input[name="transdate"]')) return;
      |      syncPeriod(el.closest('form'));
      |    });
      |  })();
      |
      |    /* =====================================================================
      |     Refresh the list after a successful form save
      |     - HTMX reports the request verb in UPPERCASE, so the check must be
      |       case-insensitive.
      |     - The form is swapped before htmx:afterRequest fires, so look up the
      |       live combo by id in the document rather than using the (detached)
      |       request element.
      |     - Fires `change` on the module combo, which reloads the list card via
      |       the combo's own hx-get.
      |     ===================================================================== */
      |  (function () {
      |    document.body.addEventListener('htmx:afterRequest', function (e) {
      |      var d = e.detail;
      |      if (!d || !d.successful) return;
      |      if (!d.requestConfig) return;
      |
      |      var verb = (d.requestConfig.verb || '').toLowerCase();
      |      if (verb !== 'put') return;
      |
      |      var el = d.elt;
      |      if (!el || !el.id) return;
      |      var formId = el.id;
      |
      |      // Defer one tick so the swap has settled and the new combo exists.
      |      setTimeout(function () {
      |        var combo = document.getElementById(formId + '-fmodule');
      |        if (combo && window.htmx) window.htmx.trigger(combo, 'change');
      |      }, 0);
      |    });
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