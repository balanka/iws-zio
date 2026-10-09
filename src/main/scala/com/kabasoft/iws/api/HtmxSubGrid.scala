package com.kabasoft.iws.api

import Html.esc

object HtmxSubGrid:

  case class Column(field: String, label: String, cssClass: String = "")

  case class Row(
                  cells:  List[String],
                  hidden: Map[String, String] = Map.empty
                )

  def render(
              id:      String,
              prefix:  String,
              fkField: String,
              fkValue: String,
              columns: List[Column],
              rows:    List[Row]
            ): String =

    // ---- header ----
    val headerCells: String =
      columns.map(c => s"<th>${esc(c.label)}</th>").mkString

    // ---- one row's hidden inputs (fk + any extra) ----
    def hiddenFor(idx: Int, extra: Map[String, String]): String =
      val fk = s"""<input type="hidden" name="$prefix.$idx.${esc(fkField)}" value="${esc(fkValue)}" />"""
      val rest = extra.map { case (k, v) =>
        s"""<input type="hidden" name="$prefix.$idx.${esc(k)}" value="${esc(v)}" />"""
      }.mkString
      fk + rest

    // ---- one body row ----
    def renderRow(row: Row, idx: Int): String =
      val cells = columns.zipAll(row.cells, null, "").map { case (col, value) =>
        val v = if value == null then "" else value
        s"""<td class="${esc(col.cssClass)}">
           |  <input class="input input-xs ${esc(col.cssClass)}"
           |         name="$prefix.$idx.${esc(col.field)}"
           |         value="${esc(v)}" />
           |</td>""".stripMargin
      }.mkString

      s"""<tr>
         |  $cells
         |  <td class="subgrid-actions">
         |    <button type="button" class="btn btn-xs btn-ghost"
         |            onclick="removeSubRow(this)">×</button>
         |    ${hiddenFor(idx, row.hidden)}
         |  </td>
         |</tr>""".stripMargin

    val bodyRows: String =
      rows.zipWithIndex.map { case (row, idx) => renderRow(row, idx) }.mkString

    // ---- the template row (index placeholder __INDEX__) ----
    val templateCells: String =
      columns.map { col =>
        s"""<td class="${esc(col.cssClass)}">
           |  <input class="input input-xs ${esc(col.cssClass)}"
           |         name="$prefix.__INDEX__.${esc(col.field)}" value="" />
           |</td>""".stripMargin
      }.mkString

    val templateRow: String =
      s"""<tr>
         |  $templateCells
         |  <td class="subgrid-actions">
         |    <button type="button" class="btn btn-xs btn-ghost"
         |            onclick="removeSubRow(this)">×</button>
         |    <input type="hidden" name="$prefix.__INDEX__.${esc(fkField)}" value="${esc(fkValue)}" />
         |  </td>
         |</tr>""".stripMargin

    // ---- outer shell ----
    s"""
       |<div class="htmx-subgrid" data-prefix="$prefix">
       |  <table class="table table-xs table-zebra">
       |    <thead>
       |      <tr>$headerCells<th></th></tr>
       |    </thead>
       |    <tbody id="$id-body">
       |      $bodyRows
       |    </tbody>
       |  </table>
       |  <button type="button" class="btn btn-xs btn-outline mt-1"
       |          onclick="addSubRow('$id', '$prefix')">+ Add</button>
       |
       |  <template id="$id-template">
       |    $templateRow
       |  </template>
       |</div>
       |""".stripMargin