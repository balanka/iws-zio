package com.kabasoft.iws.api

import Html.esc

object HtmxTable:

  case class Column(
                     key:   String,
                     label: String,
                     width: Option[String] = None
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

  def render[A](
                 path:        String,
                 columns:     List[Column],
                 p:           Page[A],
                 rowCells:    A => List[String],
                 detailUrl:   A => String,
                 cellClasses: A => List[String] = (_: A) => Nil,
                 query:       String = "",
                 rowMode:     RowMode = RowMode.Navigate,
                 maxHeight:   String = "480px"
               ): String =
    s"""
       |<div class="htmx-table-wrapper">
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
       |             hx-include="[name='sort'],[name='size']" />
       |    </div>
       |  </div>
       |
       |  <div class="table-scroll" style="max-height: $maxHeight; overflow-y: auto;">
       |    <table class="table table-zebra">
       |      <thead>
       |        <tr>${columns.map(c => headerCell(path, c, p)).mkString}</tr>
       |      </thead>
       |      <tbody>
       |        ${p.rows.map(r => row(r, rowCells, cellClasses, detailUrl, rowMode)).mkString}
       |      </tbody>
       |    </table>
       |  </div>
       |
       |  ${pager(path, p)}
       |</div>
       |""".stripMargin

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
    val style = c.width.map(w => s"""style="width:$w"""").getOrElse("")
    s"""<th $style>
       |  <a class="table-sort"
       |     data-sort-key="${esc(c.key)}"
       |     title="Click to sort. Shift+click to add to sort."
       |     hx-get="$path"
       |     hx-include="[name='sort'],[name='size'],[name='q']"
       |     hx-target="closest .htmx-table-wrapper"
       |     hx-swap="outerHTML"
       |     hx-push-url="true">${esc(c.label)}$arrow</a>
       |</th>""".stripMargin

  private def row[A](
                      r:           A,
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
      val klass = cls.lift(i).getOrElse("")
      s"""<td class="$klass">${esc(c)}</td>"""
    }.mkString}
       |</tr>""".stripMargin

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
       |              hx-include="[name='sort'],[name='q']">
       |        ${pageSizes.map(s => sizeOption(s, p.pageSize)).mkString}
       |      </select>
       |      <span>rows</span>
       |    </label>
       |  </div>
       |  <div class="pager-center">
       |    <button class="btn $disabledPrev"
       |            hx-get="$path?page=$prev"
       |            hx-include="[name='sort'],[name='size'],[name='q']"
       |            hx-target="closest .htmx-table-wrapper"
       |            hx-swap="outerHTML"
       |            hx-push-url="true">‹ Prev</button>
       |    <span class="page-info">Page ${p.page + 1} of ${p.totalPages} (${p.total} rows)</span>
       |    <button class="btn $disabledNext"
       |            hx-get="$path?page=$next"
       |            hx-include="[name='sort'],[name='size'],[name='q']"
       |            hx-target="closest .htmx-table-wrapper"
       |            hx-swap="outerHTML"
       |            hx-push-url="true">Next ›</button>
       |  </div>
       |  <div class="pager-right"></div>
       |</div>
       |""".stripMargin