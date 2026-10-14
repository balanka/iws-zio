package com.kabasoft.iws.api

import Html.esc

object HtmxTable:

  enum FilterKind:
    case None, Text, Number, Date, Select

  enum ColumnKind:
    case Text, Bool

  case class Column(
                     key:           String,
                     label:         String,
                     width:         Option[String] = None,
                     filter:        FilterKind = FilterKind.None,
                     filterOptions: List[(String, String)] = Nil,
                     kind:          ColumnKind = ColumnKind.Text,
                     resizable:     Boolean = false
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

  val pageSizes: List[Int] = List(10, 20, 40, 80)

  /** Everything a list request should carry (sort, size, q, modelid, company,
   * and every per-column filter). Used by all list interactions EXCEPT the
   * size select itself — see `carryExceptSize`.
   */
  private val carryAll: String =
    "[name='sort'],[name='size'],[name='q'],[name='modelid'],[name='company'],[data-filter-input]"

  /** Same as carryAll, minus [name='size']. The size select's own value is
   * sent by HTMX as the trigger element; including it here would duplicate.
   */
  private val carryExceptSize: String =
    "[name='sort'],[name='q'],[name='modelid'],[name='company'],[data-filter-input]"

  def render[A](
                 path:         String,
                 columns:      List[Column],
                 p:            Page[A],
                 rowCells:     A => List[String],
                 detailUrl:    A => String,
                 cellClasses:  A => List[String] = (_: A) => Nil,
                 query:        String = "",
                 rowMode:      RowMode = RowMode.Navigate,
                 maxHeight:    String = "480px",
                 filterValues: Map[String, String] = Map.empty
               ): String =
    val hasFilters   = columns.exists(_.filter != FilterKind.None)
    val hasResizable = columns.exists(_.resizable)

    val filterRow =
      if hasFilters then
        s"""<tr class="table-filter-row">${columns.map(c => filterCell(path, c, filterValues)).mkString}</tr>"""
      else ""

    val tableCls = if hasResizable then "table table-zebra htmx-table-fixed" else "table table-zebra"

    s"""
       |<div class="htmx-table-wrapper" data-table-key="$path">
       |  <div class="table-toolbar">
       |    <input type="hidden" name="sort" value="${esc(p.encodedSort)}" />
       |    <div class="table-search-wrapper">
       |      <input type="search"
       |             id="table-search"
       |             class="input input-xs input-bordered w-full max-w-xs"
       |             name="q"
       |             value="${esc(query)}"
       |             placeholder="Search…"
       |             hx-preserve="true"
       |             hx-get="$path"
       |             hx-trigger="keyup changed delay:300ms, search"
       |             hx-target="closest .htmx-table-wrapper"
       |             hx-swap="outerHTML"
       |             hx-include="$carryAll" />
       |    </div>
       |  </div>
       |
       |  <div class="table-scroll" style="max-height: $maxHeight; overflow-y: auto;">
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
    val inner =
      c.filter match
        case FilterKind.None => ""
        case FilterKind.Text =>
          s"""<input type="search"
             |       class="input input-xs table-filter-input"
             |       data-filter-input
             |       name="${esc(c.key)}"
             |       value="${esc(cur)}"
             |       placeholder="…"
             |       hx-get="$path"
             |       hx-trigger="keyup changed delay:400ms, search"
             |       hx-target="closest .htmx-table-wrapper"
             |       hx-swap="outerHTML"
             |       hx-include="$carryAll" />""".stripMargin

        case FilterKind.Number =>
          s"""<input type="number"
             |       class="input input-xs table-filter-input num"
             |       data-filter-input
             |       name="${esc(c.key)}"
             |       value="${esc(cur)}"
             |       placeholder="…"
             |       hx-get="$path"
             |       hx-trigger="keyup changed delay:400ms, change"
             |       hx-target="closest .htmx-table-wrapper"
             |       hx-swap="outerHTML"
             |       hx-include="$carryAll" />""".stripMargin

        case FilterKind.Date =>
          s"""<input type="date"
             |       class="input input-xs table-filter-input"
             |       data-filter-input
             |       name="${esc(c.key)}"
             |       value="${esc(cur)}"
             |       hx-get="$path"
             |       hx-trigger="change"
             |       hx-target="closest .htmx-table-wrapper"
             |       hx-swap="outerHTML"
             |       hx-include="$carryAll" />""".stripMargin

        case FilterKind.Select =>
          val opts = (("" -> "Any") :: c.filterOptions).map { case (v, l) =>
            val sel = if v == cur then " selected" else ""
            s"""<option value="${esc(v)}"$sel>${esc(l)}</option>"""
          }.mkString
          s"""<select class="select select-xs table-filter-input"
             |        data-filter-input
             |        name="${esc(c.key)}"
             |        hx-get="$path"
             |        hx-trigger="change"
             |        hx-target="closest .htmx-table-wrapper"
             |        hx-swap="outerHTML"
             |        hx-include="$carryAll">
             |  $opts
             |</select>""".stripMargin

    s"""<th class="table-filter-cell" data-col-key="${esc(c.key)}" $style>$inner</th>"""

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
       |  <a class="table-sort"
       |     data-sort-key="${esc(c.key)}"
       |     title="Click to sort. Shift+click to add to sort."
       |     hx-get="$path"
       |     hx-include="$carryAll"
       |     hx-target="closest .htmx-table-wrapper"
       |     hx-swap="outerHTML"
       |     hx-push-url="true">${esc(c.label)}$arrow</a>
       |  $handle
       |</th>""".stripMargin

  // ── body row ───────────────────────────────────────────────────────────

  private def row[A](
                      r:           A,
                      columns:     List[Column],
                      rowCells:    A => List[String],
                      cellClasses: A => List[String],
                      detailUrl:   A => String,
                      mode:        RowMode
                    ): String =
    val attrs = mode match
      case RowMode.Navigate =>
        s"""onclick="window.location.href='${detailUrl(r)}'" style="cursor:pointer;" """
      case RowMode.Fetch(target) =>
        s"""hx-get="${detailUrl(r)}" hx-target="$target" hx-swap="outerHTML" style="cursor:pointer;" """

    val cells = rowCells(r)
    val cls   = cellClasses(r)

    s"""<tr class="table-row" $attrs>
       |  ${cells.zipWithIndex.map { case (c, i) =>
      val userCls = cls.lift(i).getOrElse("")
      val col     = columns.lift(i)

      val (cellCls, inner) = col.map(_.kind).getOrElse(ColumnKind.Text) match
        case ColumnKind.Bool =>
          val checked = if c == "true" then " checked" else ""
          val klass   = (userCls + " bool-cell").trim
          (klass, s"""<input type="checkbox" tabindex="-1" aria-readonly="true"$checked />""")
        case ColumnKind.Text =>
          (userCls, esc(c))

      s"""<td class="$cellCls">$inner</td>"""
    }.mkString}
       |</tr>""".stripMargin

  // ── pager ──────────────────────────────────────────────────────────────

  private def pager[A](path: String, p: Page[A]): String =
    val prev = math.max(0, p.page - 1)
    val next = math.min(p.totalPages - 1, p.page + 1)
    val disabledPrev = if p.page == 0 then "disabled" else ""
    val disabledNext = if p.page >= p.totalPages - 1 then "disabled" else ""

    s"""
       |<div class="table-pager">
       |  <div class="pager-left">
       |    <label class="page-size">
       |      <span>Show</span>
       |      <select class="select select-xs"
       |              name="size"
       |              hx-get="$path"
       |              hx-trigger="change"
       |              hx-target="closest .htmx-table-wrapper"
       |              hx-swap="outerHTML"
       |              hx-include="$carryExceptSize">
       |        ${pageSizes.map(s => sizeOption(s, p.pageSize)).mkString}
       |      </select>
       |      <span>rows</span>
       |    </label>
       |  </div>
       |  <div class="pager-center">
       |    <button class="btn $disabledPrev"
       |            hx-get="$path?page=$prev"
       |            hx-include="$carryAll"
       |            hx-target="closest .htmx-table-wrapper"
       |            hx-swap="outerHTML"
       |            hx-push-url="true">‹ Prev</button>
       |    <span class="page-info">Page ${p.page + 1} of ${p.totalPages} (${p.total} rows)</span>
       |    <button class="btn $disabledNext"
       |            hx-get="$path?page=$next"
       |            hx-include="$carryAll"
       |            hx-target="closest .htmx-table-wrapper"
       |            hx-swap="outerHTML"
       |            hx-push-url="true">Next ›</button>
       |  </div>
       |  <div class="pager-right"></div>
       |</div>
       |""".stripMargin