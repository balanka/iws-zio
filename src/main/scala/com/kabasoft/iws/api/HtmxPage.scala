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
      |  /* Multi-column sort */
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
      |  /* Collapsible cards */
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
      |  /* Show / hide the table search field */
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
      |  /* Density selector */
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
      |  /* Form edit-mode toggle */
      |  (function () {
      |    function syncLabel() {
      |      var btn = document.getElementById('toggle-edit-btn');
      |      if (!btn) return;
      |      var form = document.querySelector(btn.dataset.editTarget);
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
      |      var form = document.querySelector(btn.dataset.editTarget);
      |      if (!form) return;
      |      form.classList.toggle('form-readonly');
      |      syncLabel();
      |    });
      |
      |    document.body.addEventListener('htmx:afterSwap', function (e) {
      |      var t = e.detail && e.detail.target;
      |      if (t && t.id && t.id.endsWith('-form')) syncLabel();
      |    });
      |    syncLabel();
      |  })();
      |  
      |    /* Sub-grid add / remove */
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

 