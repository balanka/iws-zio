package com.kabasoft.iws.api

object HtmxTitleUpdates:

  def fmoduleTitleUpdate(
                          formId:  String,
                          titleId: String,
                          cardId:  String,
                          company: String
                        ): String =
    val c = Html.esc(company)
    s"""if (event.target.id === '$formId-fmodule') {
       |  console.log('[fmodule change] fired: formId=$formId, titleId=$titleId, cardId=$cardId');
       |  var sel = event.target.options[event.target.selectedIndex];
       |  var fmoduleName = sel ? sel.textContent.trim() : '';
       |  var full = '$c/' + fmoduleName;
       |  var t = document.getElementById('$titleId');
       |  if (t) t.textContent = full;
       |  var c2 = document.getElementById('$cardId');
       |  if (c2) c2.textContent = full;
       |  document.title = full;
       |}""".stripMargin