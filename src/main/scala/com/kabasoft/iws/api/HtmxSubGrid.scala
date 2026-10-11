package com.kabasoft.iws.api

import Html.esc

object HtmxSubGrid:

  enum CellKind:
    case Text, Number, Money, Date, Select, Computed

  case class Column(
                     field:      String,
                     label:      String,
                     cssClass:   String = "",
                     options:    List[(String, String)] = Nil,
                     optionData: Map[String, Map[String, String]] = Map.empty,  // value → extra attrs
                     readonly:   Boolean = false,
                     kind:       CellKind = CellKind.Text,
                     expr:       String = ""
                   )

  case class Row(
                  cells:  List[String],
                  hidden: Map[String, String] = Map.empty
                )

  def render(
              id:         String,
              prefix:     String,
              fkField:    String,
              fkValue:    String,
              columns:    List[Column],
              rows:       List[Row],
              readonly:   Boolean = false,
              totalField: String = ""
            ): String =
    if readonly then renderReadonly(prefix, columns, rows)
    else            renderEditable(id, prefix, fkField, fkValue, columns, rows, totalField)

  // ── read-only (whole grid) ──────────────────────────────────────────────

  private def renderReadonly(prefix: String, columns: List[Column], rows: List[Row]): String =
    val headerCells = columns.map(c => s"<th>${esc(c.label)}</th>").mkString
    val bodyRows =
      if rows.isEmpty then
        s"""<tr><td colspan="${columns.size}" class="subgrid-empty">— none —</td></tr>"""
      else
        rows.map { row =>
          val cells = columns.zipAll(row.cells, null, "").map { case (col, value) =>
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
                              id:         String,
                              prefix:     String,
                              fkField:    String,
                              fkValue:    String,
                              columns:    List[Column],
                              rows:       List[Row],
                              totalField: String
                            ): String =
    val headerCells = columns.map(c => s"<th>${esc(c.label)}</th>").mkString

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
          s"""<input class="input input-xs num ${esc(col.cssClass)}"
             |       type="number" step="0.01"
             |       name="$prefix.$idx.${esc(col.field)}"
             |       value="${esc(value)}" />""".stripMargin

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
          s"""<span class="calc-cell num ${esc(col.cssClass)}"
             |      data-expr="${esc(col.expr)}"
             |      tabindex="-1"
             |      aria-readonly="true"
             |      onmousedown="return false"
             |      ondblclick="return false">${esc(value)}</span>
             |<input type="hidden"
             |       tabindex="-1"
             |       aria-hidden="true"
             |       data-calc
             |       name="$prefix.$idx.${esc(col.field)}"
             |       value="${esc(value)}" />""".stripMargin

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

    def renderRow(row: Row, idx: Int): String =
      val cells = columns.zipAll(row.cells, null, "").map { case (col, value) =>
        val v = if value == null then "" else value
        s"""<td class="${esc(tdClass(col))}">${cell(col, idx.toString, v)}</td>"""
      }.mkString

      s"""<tr>
         |  $cells
         |  <td class="subgrid-actions">
         |    <button type="button" class="btn btn-xs btn-ghost"
         |            onclick="removeSubRow(this)">×</button>
         |    ${hiddenFor(idx.toString, row.hidden)}
         |  </td>
         |</tr>""".stripMargin

    val bodyRows = rows.zipWithIndex.map { case (row, idx) => renderRow(row, idx) }.mkString

    val templateCells = columns.map { col =>
      val initial =
        col.kind match
          case CellKind.Computed => "0.00"
          case CellKind.Date     => ""
          case _                 => ""
      s"""<td class="${esc(tdClass(col))}">${cell(col, "__INDEX__", initial)}</td>"""
    }.mkString

    val templateRow =
      s"""<tr>
         |  $templateCells
         |  <td class="subgrid-actions">
         |    <button type="button" class="btn btn-xs btn-ghost"
         |            onclick="removeSubRow(this)">×</button>
         |    <input type="hidden" name="$prefix.__INDEX__.${esc(fkField)}" value="${esc(fkValue)}" />
         |  </td>
         |</tr>""".stripMargin

    val totalAttr = if totalField.nonEmpty then s""" data-total-field="$totalField"""" else ""

    s"""
       |<div class="htmx-subgrid" data-prefix="$prefix" data-fk-field="$fkField"$totalAttr>
       |  <table class="table table-xs table-zebra">
       |    <thead><tr>$headerCells<th></th></tr></thead>
       |    <tbody id="$id-body">$bodyRows</tbody>
       |  </table>
       |  <button type="button" class="btn btn-xs btn-outline mt-1"
       |          onclick="addSubRow('$id', '$prefix')">+ Add line</button>
       |
       |  <template id="$id-template">$templateRow</template>
       |</div>
       |""".stripMargin