package com.kabasoft.iws.api

import Html.esc

object HtmxTable:

  enum FilterKind:
    case None, Text, Number, Date, Select

  enum ColumnKind:
    case Text, Bool

  case class Column(
                     key: String,
                     label: String,
                     width: Option[String] = None,
                     filter: FilterKind = FilterKind.None,
                     filterOptions: List[(String, String)] = Nil,
                     kind: ColumnKind = ColumnKind.Text,
                     resizable: Boolean = false,
                     cssClass: String = ""
                   )

  case class Page[A](
                      rows:     List[A],
                      page:     Int,
                      pageSize: Int,
                      total:    Long,
                      sort:     List[(String, String)] = Page.defaultSort
                    ):
    def totalPages: Int = math.max(1, ((total + pageSize - 1) / pageSize).toInt)
    def encodedSort: String = sort.map((k, d) => s"$k:$d").mkString(",")

  object Page:
    val defaultSort: List[(String, String)] = List(("id", "asc"))

    def parseSort(raw: String): List[(String, String)] =
      raw.split(",").toList.filter(_.nonEmpty).flatMap { part =>
        val i = part.indexOf(':')
        if i < 0 then Some(part.trim -> "asc")
        else
          val k = part.substring(0, i).trim
          val d = part.substring(i + 1).trim
          if k.isEmpty then None
          else Some(k -> (if d == "desc" then "desc" else "asc"))
      }

  enum RowMode:
    case Navigate
    case Fetch(target: String)

  val pageSizes: List[Int] = List(10, 20, 40, 80, 120)

  /** Everything a list request should carry (sort, size, q, modelid, company,
   * and every per-column filter). Used by all list interactions EXCEPT the
   * size select itself — see `carryExceptSize`.
   */

  private val carryAll: String =
    "closest .htmx-daisy-table-wrapper, [name='modelid'], [name='company']"

  private val carryExceptSize: String = carryAll

  /** Stable, DOM-safe id fragment for a given table path. */
  private def keyFragment(path: String): String =
    path.replaceAll("[^a-zA-Z0-9]", "-")

  def render[A](
                 path:         String,
                 columns:      List[Column],
                 p:            Page[A],
                 rowCells:     A => List[String],
                 detailUrl:    A => String,
                 cellClasses:  A => List[String] = (_: A) => Nil,
                 query:        String = "",
                 rowMode:      RowMode = RowMode.Navigate,
                 //maxHeight:    String = "480px",
                 maxHeight:String = "calc(100vh - 500px)",
                 filterValues: Map[String, String] = Map.empty
               ): String =
    val hasFilters   = columns.exists(_.filter != FilterKind.None)
    val hasResizable = columns.exists(_.resizable)

    val frag      = keyFragment(path)
    val searchId  = s"table-search-$frag"
    val syncSel   = s"closest .htmx-daisy-table-wrapper"

    val filterRow =
      if hasFilters then
        s"""<tr class="daisy-table-filter-row">${columns.map(c => filterCell(path, c, filterValues)).mkString}</tr>"""
      else ""

    val tableCls = if hasResizable then "daisy-table daisy-table-zebra htmx-table-fixed" else "daisy-table daisy-table-zebra"

    s"""
       |<div class="htmx-daisy-table-wrapper" data-table-key="$path">
       |  <div class="daisy-table-toolbar">
       |    <input type="hidden" name="sort" value="${esc(p.encodedSort)}" />
       |     <div id="daisy-table-search-wrapper" class="daisy-table-search-wrapper hidden">
       |        <input type="search"
       |          id="$searchId"
       |          class="daisy-input input-xs daisy-input-bordered w-full max-w-xs"
       |          name="q"
       |          value="${esc(query)}"
       |          placeholder="Search…"
       |          hx-get="$path"
       |          hx-trigger="input changed delay:300ms, search"
       |          hx-target="closest .htmx-daisy-table-wrapper"
       |          hx-swap="outerHTML"
       |          hx-sync="$syncSel:replace"
       |          hx-include="$carryAll" />
       |    </div>
       |  </div>
       |
       |  <div class="daisy-table-scroll" style="max-height: $maxHeight; overflow-y: auto;">
       |    <table class="$tableCls" data-table-key="$path">
       |      <thead>
       |        <tr>${columns.map(c => headerCell(path, c, p)).mkString}</tr>
       |        $filterRow
       |      </thead>
       |      <tbody>
       |        ${p.rows.map(r => row(r, columns, rowCells, cellClasses, detailUrl, rowMode)).mkString}
       |      </tbody>
       |    </table>
       |  </div>
       |
       |  ${pager(path, p)}
       |</div>
       |""".stripMargin

  // ── filter row cell ────────────────────────────────────────────────────

  private def filterCell(path: String, c: Column, values: Map[String, String]): String =
    val cur   = values.getOrElse(c.key, "")
    val style = c.width.map(w => s"""style="width:$w"""").getOrElse("")
    val fid   = s"filter-${keyFragment(path)}-${c.key}"

    // Shared attributes for all filter inputs:
    //  - hx-preserve keeps the live DOM node across outerHTML swaps so focus,
    //    caret position and the user's typed value survive re-renders.
    //  - hx-sync aborts any in-flight request when a new one starts, which
    //    prevents a slow "67" response from overwriting a fast "6786v".
    val common =
      s"""data-filter-input
         |id="$fid"
         |name="${esc(c.key)}"
         |hx-get="$path"
         |hx-target="closest .htmx-daisy-table-wrapper"
         |hx-swap="outerHTML"
         |hx-sync="closest .htmx-daisy-table-wrapper:replace"
         |hx-include="$carryAll"
         |""".stripMargin

    val inner =
      c.filter match
        case FilterKind.None => ""

        case FilterKind.Text =>
          s"""<input type="search"
             |       class="daisy-input daisy-input-xs daisy-table-filter-input"
             |       $common
             |       value="${esc(cur)}"
             |       placeholder="…"
             |       hx-trigger="input changed delay:400ms, search" />""".stripMargin

        case FilterKind.Number =>
          // `input` fires on paste and clear too. `change` fires on blur/Enter
          // so a complete number is applied without waiting for the debounce.
          s"""<input type="number"
             |       class="daisy-input daisy-input-xs daisy-table-filter-input num"
             |       $common
             |       value="${esc(cur)}"
             |       placeholder="…"
             |       hx-trigger="input changed delay:500ms, change, search" />""".stripMargin

        case FilterKind.Date =>
          s"""<input type="date"
             |       class="daisy-input daisy-input-xs daisy-table-filter-input"
             |       $common
             |       value="${esc(cur)}"
             |       hx-trigger="change" />""".stripMargin

        case FilterKind.Select =>
          val opts = (("" -> "Any") :: c.filterOptions).map { case (v, l) =>
            val sel = if v == cur then " selected" else ""
            s"""<option value="${esc(v)}"$sel>${esc(l)}</option>"""
          }.mkString
          s"""<select class="daisy-select daisy-select-xs daisy-table-filter-input"
             |        $common
             |        hx-trigger="change">
             |  $opts
             |</select>""".stripMargin

    s"""<th class="daisy-table-filter-cell" data-col-key="${esc(c.key)}" $style>$inner</th>"""

  // ── header cell ────────────────────────────────────────────────────────

  private def sizeOption(size: Int, current: Int): String =
    val sel = if size == current then " selected" else ""
    s"""<option value="$size"$sel>$size</option>"""

  private def headerCell[A](path: String, c: Column, p: Page[A]): String =
    val sortIdx = p.sort.indexWhere(_._1 == c.key)
    val arrow =
      if sortIdx < 0 then ""
      else
        val (_, dir) = p.sort(sortIdx)
        val glyph = if dir == "asc" then "▲" else "▼"
        if p.sort.length == 1 then s" $glyph"
        else s" ${sortIdx + 1}$glyph"
    val style  = c.width.map(w => s"""style="width:$w"""").getOrElse("")
    val resiz  = if c.resizable then " resizable-th" else ""
    val handle =
      if c.resizable then
        s"""<span class="col-resizer" data-col-key="${esc(c.key)}" title="Drag to resize"></span>"""
      else ""
    s"""<th class="th-${esc(c.key)}$resiz" data-col-key="${esc(c.key)}" $style>
       |  <a class="daisy-table-sort"
       |     data-sort-key="${esc(c.key)}"
       |     title="Click to sort. Shift+click to add to sort."
       |     hx-get="$path"
       |     hx-include="$carryAll"
       |     hx-target="closest .htmx-daisy-table-wrapper"
       |     hx-swap="outerHTML"
       |     hx-push-url="true">${esc(c.label)}$arrow</a>
       |  $handle
       |</th>""".stripMargin

  // ── body row ───────────────────────────────────────────────────────────
  private def row[A](
                      r: A,
                      columns: List[Column],
                      rowCells: A => List[String],
                      cellClasses: A => List[String],
                      detailUrl: A => String,
                      mode: RowMode
                    ): String =
    val attrs = mode match
      case RowMode.Navigate =>
        s"""onclick="window.location.href='${detailUrl(r)}'" style="cursor:pointer;" """
      case RowMode.Fetch(target) =>
        s"""hx-get="${detailUrl(r)}" hx-target="$target" hx-swap="outerHTML" style="cursor:pointer;" """

    val cells = rowCells(r)
    val cls = cellClasses(r)

    s"""<tr class="daisy-table-row" $attrs>
       |  ${
      cells.zipWithIndex.map { case (c, i) =>
        val userCls = cls.lift(i).getOrElse("")
        val col = columns.lift(i)
        val colCss = col.map(_.cssClass).getOrElse("")
        val combinedCls = (userCls + " " + colCss).trim

        val (cellCls, inner) = col.map(_.kind).getOrElse(ColumnKind.Text) match
          case ColumnKind.Bool =>
            val checked = if c == "true" then " checked" else ""
            val klass = (combinedCls + " bool-cell").trim
            (klass, s"""<input type="checkbox" tabindex="-1" aria-readonly="true"$checked />""")
          case ColumnKind.Text =>
            (combinedCls, esc(c))

        s"""<td class="$cellCls" data-col-key="${esc(col.map(_.key).getOrElse(""))}">$inner</td>"""
      }.mkString
    }
       |</tr>""".stripMargin


  // ── pager ──────────────────────────────────────────────────────────────

  private def pager[A](path: String, p: Page[A]): String =
    val prev = math.max(0, p.page - 1)
    val next = math.min(p.totalPages - 1, p.page + 1)
    val disabledPrev = if p.page == 0 then "disabled" else ""
    val disabledNext = if p.page >= p.totalPages - 1 then "disabled" else ""

    s"""
       |<div class="daisy-table-pager">
       |  <div class="pager-left">
       |    <label class="page-size">
       |      <span>Show</span>
       |      <select class="daisy-select daisy-select-xs"
       |              name="size"
       |              hx-get="$path"
       |              hx-trigger="change"
       |              hx-target="closest .htmx-daisy-table-wrapper"
       |              hx-swap="outerHTML"
       |              hx-include="$carryExceptSize">
       |        ${pageSizes.map(s => sizeOption(s, p.pageSize)).mkString}
       |      </select>
       |      <span>rows</span>
       |    </label>
       |  </div>
       |  <div class="pager-center">
       |    <button class="daisy-btn $disabledPrev"
       |            hx-get="$path?page=$prev"
       |            hx-include="$carryAll"
       |            hx-target="closest .htmx-daisy-table-wrapper"
       |            hx-swap="outerHTML"
       |            hx-push-url="true">‹ Prev</button>
       |    <span class="page-info">Page ${p.page + 1} of ${p.totalPages} (${p.total} rows)</span>
       |    <button class="daisy-btn $disabledNext"
       |            hx-get="$path?page=$next"
       |            hx-include="$carryAll"
       |            hx-target="closest .htmx-daisy-table-wrapper"
       |            hx-swap="outerHTML"
       |            hx-push-url="true">Next ›</button>
       |  </div>
       |  <div class="pager-right"></div>
       |</div>
       |""".stripMargin