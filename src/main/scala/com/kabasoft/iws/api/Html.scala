package com.kabasoft.iws.api

object Html:
  def esc(s: String | Null): String =
    if (s == null) ""
    else s.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;")