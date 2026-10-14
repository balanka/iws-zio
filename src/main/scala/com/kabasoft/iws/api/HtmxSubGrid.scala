package com.kabasoft.iws.api

import Html.esc

object HtmxSubGrid:

  enum CellKind:
    case Text, Number, Money, Date, Select, Computed

  case class Column(field: String,
                     label: String,
                     cssClass: String = "",
                     options: List[(String, String)] = Nil,
                     optionData: Map[String, Map[String, String]] = Map.empty,
                     readonly: Boolean = false,
                     kind: CellKind = CellKind.Text,
                     expr: String = "",
                     hidden: Boolean = false,
                     resizable: Boolean = false,
                     printHidden: Boolean = false)


  case class Row(cells:  List[String], hidden: Map[String, String] = Map.empty)



  /** A single cell in the subgrid's <tfoot> row.
   * `colspan` must sum, across all footer cells, to the number of
   * visible columns in the subgrid. HtmxSubGrid adds one trailing
   * action cell so the tfoot row lines up with the header + body.
   */
  case class FooterCell(colspan: Int, content: String = "", css: String = "", printHidden: Boolean = false)

  def render(
              id:             String,
              prefix:         String,
              fkField:        String,
              fkValue:        String,
              columns:        List[Column],
              rows:           List[Row],
              readonly:       Boolean = false,
              totalField:     String = "",
              currencySymbol: String = "",
              footers:        List[FooterCell] = Nil
            ): String =
    if readonly then renderReadonly(prefix, columns, rows)
    else            renderEditable(id, prefix, fkField, fkValue, columns, rows, currencySymbol, footers)

  // ── read-only (whole grid) ──────────────────────────────────────────────

  private def renderReadonly(prefix: String, columns: List[Column], rows: List[Row]): String =
    val visible = columns.filterNot(_.hidden)
    val headerCells = visible.map(c => s"<th>${esc(c.label)}</th>").mkString
    val bodyRows =
      if rows.isEmpty then
        s"""<tr><td colspan="${visible.size}" class="subgrid-empty">— none —</td></tr>"""
      else
        rows.map { row =>
          val cells = columns.zipAll(row.cells, null, "").collect {
            case (col, value) if !col.hidden =>
              val v = if value == null then "" else value
              s"""<td class="${esc(col.cssClass)}">${esc(v)}</td>"""
          }.mkString
          s"<tr>$cells</tr>"
        }.mkString

    s"""
       |<div class="htmx-subgrid htmx-subgrid-readonly" data-prefix="$prefix">
       |  <table class="table table-xs table-zebra">
       |    <thead><tr>$headerCells</tr></thead>
       |    <tbody>$bodyRows</tbody>
       |  </table>
       |</div>
       |""".stripMargin

  // ── editable ────────────────────────────────────────────────────────────

  private def renderEditable(
                              id:             String,
                              prefix:         String,
                              fkField:        String,
                              fkValue:        String,
                              columns:        List[Column],
                              rows:           List[Row],
                              currencySymbol: String,
                              footers:        List[FooterCell]
                            ): String =
    val visibleColumns = columns.filterNot(_.hidden)
    val hasResizable   = visibleColumns.exists(_.resizable)

    val tableCls =
      if hasResizable then "table table-xs table-zebra htmx-table-fixed"
      else                  "table table-xs table-zebra"

    val headerCells = visibleColumns.map { c =>
      val clsList = List(
        s"th-${c.field}",
        if c.resizable then "resizable-th" else "",
        if c.printHidden then "print-hide" else ""
      ).filter(_.nonEmpty).mkString(" ")

      val handle =
        if c.resizable then
          s"""<span class="col-resizer" data-col-key="${esc(c.field)}" title="Drag to resize"></span>"""
        else ""

      s"""<th class="$clsList" data-col-key="${esc(c.field)}">${esc(c.label)}$handle</th>"""
    }.mkString

    def hiddenFor(idx: String, extra: Map[String, String]): String =
      val fk = s"""<input type="hidden" name="$prefix.$idx.${esc(fkField)}" value="${esc(fkValue)}" />"""
      val rest = extra.map { case (k, v) =>
        s"""<input type="hidden" name="$prefix.$idx.${esc(k)}" value="${esc(v)}" />"""
      }.mkString
      fk + rest

    def optionAttrs(col: Column, v: String): String =
      col.optionData.get(v) match
        case Some(attrs) if attrs.nonEmpty =>
          attrs.map { case (k, val_) => s""" data-${esc(k)}="${esc(val_)}"""" }.mkString
        case _ => ""

    def cell(col: Column, idx: String, value: String): String =
      col.kind match
        case CellKind.Select =>
          val opts = col.options.map { case (v, label) =>
            val sel = if v == value then " selected" else ""
            s"""<option value="${esc(v)}"$sel${optionAttrs(col, v)}>${esc(label)}</option>"""
          }.mkString
          s"""<select class="select select-xs ${esc(col.cssClass)}"
             |        name="$prefix.$idx.${esc(col.field)}">
             |  $opts
             |</select>""".stripMargin

        case CellKind.Money =>
          s"""<span class="input-money">
             |  <input class="input input-xs num ${esc(col.cssClass)}"
             |         type="number" step="0.01"
             |         name="$prefix.$idx.${esc(col.field)}"
             |         value="${esc(value)}" />
             |  <span class="currency-suffix">${esc(currencySymbol)}</span>
             |</span>""".stripMargin

        case CellKind.Number =>
          s"""<input class="input input-xs num ${esc(col.cssClass)}"
             |       type="number" step="1"
             |       name="$prefix.$idx.${esc(col.field)}"
             |       value="${esc(value)}" />""".stripMargin

        case CellKind.Date =>
          s"""<input class="input input-xs ${esc(col.cssClass)}"
             |       type="date"
             |       name="$prefix.$idx.${esc(col.field)}"
             |       value="${esc(value)}" />""".stripMargin

        case CellKind.Computed =>
          s"""<span class="input-money calc-money">
             |  <span class="calc-cell num ${esc(col.cssClass)}"
             |        data-expr="${esc(col.expr)}"
             |        tabindex="-1"
             |        aria-readonly="true"
             |        onmousedown="return false"
             |        ondblclick="return false">${esc(value)}</span>
             |  <span class="currency-suffix">${esc(currencySymbol)}</span>
             |  <input type="hidden"
             |         tabindex="-1"
             |         aria-hidden="true"
             |         data-calc
             |         name="$prefix.$idx.${esc(col.field)}"
             |         value="${esc(value)}" />
             |</span>""".stripMargin

        case CellKind.Text if col.readonly =>
          s"""<input class="input input-xs ${esc(col.cssClass)} fk-cell"
             |       tabindex="-1"
             |       name="$prefix.$idx.${esc(col.field)}"
             |       value="${esc(value)}" readonly />""".stripMargin

        case CellKind.Text =>
          s"""<input class="input input-xs ${esc(col.cssClass)}"
             |       name="$prefix.$idx.${esc(col.field)}"
             |       value="${esc(value)}" />""".stripMargin

    def tdClass(col: Column): String =
      val base = col.cssClass
      if col.kind == CellKind.Computed then s"$base calc-cell-wrap".trim
      else base

    def hiddenInput(col: Column, idx: String, value: String): String =
      val v = if value == null then "" else value
      s"""<input type="hidden" name="$prefix.$idx.${esc(col.field)}" value="${esc(v)}" />"""

    def renderRow(row: Row, idx: Int): String =
      val visibleCells = columns.zipAll(row.cells, null, "").collect {
        case (col, value) if !col.hidden =>
          val v = if value == null then "" else value
          val printCls = if col.printHidden then " print-hide" else ""
          s"""<td class="${esc(tdClass(col))}$printCls">${cell(col, idx.toString, v)}</td>"""
      }.mkString

      val hiddenCells = columns.zipAll(row.cells, null, "").collect {
        case (col, value) if col.hidden =>
          hiddenInput(col, idx.toString, value)
      }.mkString

      s"""<tr>
         |  $visibleCells
         |  <td class="subgrid-actions">
         |    <button type="button" class="btn btn-xs btn-ghost"
         |            onclick="removeSubRow(this)">×</button>
         |    ${hiddenFor(idx.toString, row.hidden)}$hiddenCells
         |  </td>
         |</tr>""".stripMargin

    val bodyRows = rows.zipWithIndex.map { case (row, idx) => renderRow(row, idx) }.mkString

    val templateVisible = columns.filterNot(_.hidden).map { col =>
      val initial =
        if col.field == "id" then "-1"
        else
          col.kind match
            case CellKind.Computed => "0.00"
            case CellKind.Date     => ""
            case _                 => ""
      s"""<td class="${esc(tdClass(col))}">${cell(col, "__INDEX__", initial)}</td>"""
    }.mkString

    val templateHidden = columns.filter(_.hidden).map { col =>
      hiddenInput(col, "__INDEX__", "")
    }.mkString

    val templateRow =
      s"""<tr>
         |  $templateVisible
         |  <td class="subgrid-actions">
         |    <button type="button" class="btn btn-xs btn-ghost"
         |            onclick="removeSubRow(this)">×</button>
         |    <input type="hidden" name="$prefix.__INDEX__.${esc(fkField)}" value="${esc(fkValue)}" />
         |    $templateHidden
         |  </td>
         |</tr>""".stripMargin
    val tfootHtml =
      if footers.isEmpty then ""
      else
        val cells = footers.map { f =>
          val cls = List(f.css, if f.printHidden then "print-hide" else "")
            .filter(_.nonEmpty).mkString(" ")
          s"""<td colspan="${f.colspan}" class="${esc(cls)}">${f.content}</td>"""
        }.mkString
        s"""<tfoot><tr class="subgrid-foot-row">$cells<td class="subgrid-actions"></td></tr></tfoot>"""
    s"""
       |<div class="htmx-subgrid"
       |     data-prefix="$prefix"
       |     data-grid-id="$id"
       |     data-fk-field="$fkField">
       |  <table class="$tableCls" data-table-key="$id">
       |    <thead><tr>$headerCells<th class="subgrid-actions" data-col-key="__actions"></th></tr></thead>
       |    <tbody id="$id-body">$bodyRows</tbody>
       |    $tfootHtml
       |  </table>
       |  <template id="$id-template">$templateRow</template>
       |</div>
       |""".stripMargin