package com.kabasoft.iws.api

import Html.esc

object HtmxSubGrid:

  case class Column(
                     field:    String,
                     label:    String,
                     cssClass: String = "",
                     options:  List[(String, String)] = Nil,
                     readonly: Boolean = false
                   )

  case class Row(
                  cells:  List[String],
                  hidden: Map[String, String] = Map.empty
                )

  def render(
              id:       String,
              prefix:   String,
              fkField:  String,
              fkValue:  String,
              columns:  List[Column],
              rows:     List[Row],
              readonly: Boolean = false
            ): String =
    if readonly then renderReadonly(prefix, columns, rows)
    else            renderEditable(id, prefix, fkField, fkValue, columns, rows)

  // ── read-only (whole grid) ──────────────────────────────────────────────

  private def renderReadonly(
                              prefix:  String,
                              columns: List[Column],
                              rows:    List[Row]
                            ): String =
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

  // ── editable (grid has inputs + fk hidden fields) ───────────────────────

  private def renderEditable(
                              id:      String,
                              prefix:  String,
                              fkField: String,
                              fkValue: String,
                              columns: List[Column],
                              rows:    List[Row]
                            ): String =
    val headerCells = columns.map(c => s"<th>${esc(c.label)}</th>").mkString

    def hiddenFor(idx: Int, extra: Map[String, String]): String =
      val fk = s"""<input type="hidden" name="$prefix.$idx.${esc(fkField)}" value="${esc(fkValue)}" />"""
      val rest = extra.map { case (k, v) =>
        s"""<input type="hidden" name="$prefix.$idx.${esc(k)}" value="${esc(v)}" />"""
      }.mkString
      fk + rest

    def cellInput(col: Column, idx: String, value: String): String =
      if col.options.nonEmpty then
        // Dropdown
        val opts = col.options.map { case (v, label) =>
          val sel = if v == value then " selected" else ""
          s"""<option value="${esc(v)}"$sel>${esc(label)}</option>"""
        }.mkString
        s"""<select class="select select-xs ${esc(col.cssClass)}"
           |        name="$prefix.$idx.${esc(col.field)}">
           |  $opts
           |</select>""".stripMargin
      else if col.readonly then
        // Read-only text input — visible, not editable, still submitted
        s"""<input class="input input-xs ${esc(col.cssClass)} fk-cell"
           |       name="$prefix.$idx.${esc(col.field)}"
           |       value="${esc(value)}" readonly />""".stripMargin
      else
        // Normal editable text input
        s"""<input class="input input-xs ${esc(col.cssClass)}"
           |       name="$prefix.$idx.${esc(col.field)}"
           |       value="${esc(value)}" />""".stripMargin

    def renderRow(row: Row, idx: Int): String =
      val cells = columns.zipAll(row.cells, null, "").map { case (col, value) =>
        val v = if value == null then "" else value
        s"""<td class="${esc(col.cssClass)}">${cellInput(col, idx.toString, v)}</td>"""
      }.mkString

      s"""<tr>
         |  $cells
         |  <td class="subgrid-actions">
         |    <button type="button" class="btn btn-xs btn-ghost"
         |            onclick="removeSubRow(this)">×</button>
         |    ${hiddenFor(idx, row.hidden)}
         |  </td>
         |</tr>""".stripMargin

    val bodyRows = rows.zipWithIndex.map { case (row, idx) => renderRow(row, idx) }.mkString

    val templateCells = columns.map { col =>
      s"""<td class="${esc(col.cssClass)}">${cellInput(col, "__INDEX__", "")}</td>"""
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

    s"""
       |<div class="htmx-subgrid" data-prefix="$prefix" data-fk-field="$fkField">
       |  <table class="table table-xs table-zebra">
       |    <thead><tr>$headerCells<th></th></tr></thead>
       |    <tbody id="$id-body">$bodyRows</tbody>
       |  </table>
       |  <button type="button" class="btn btn-xs btn-outline mt-1"
       |          onclick="addSubRow('$id', '$prefix')">+ Add</button>
       |
       |  <template id="$id-template">$templateRow</template>
       |</div>
       |""".stripMargin