package com.kabasoft.iws.api

object HtmxTitleUpdates:

  /**
   * Builds an hx-on:change JS body for a form.
   * Fires only when the select with id `${formId}-fmodule` changes,
   * and updates the element with id `titleId` to `${company}/${fmoduleName}`.
   */
  def fmoduleTitleUpdate(formId: String, titleId: String, company: String): String =
    s"""if (event.target.id === '$formId-fmodule') {
       |  var sel = event.target.options[event.target.selectedIndex];
       |  var fmoduleName = sel ? sel.textContent.trim() : '';
       |  var titleEl = document.getElementById('$titleId');
       |  if (titleEl) titleEl.textContent = '${Html.esc(company)}/' + fmoduleName;
       |}""".stripMargin