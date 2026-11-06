package com.kabasoft.iws.api

/**
 * Shared logic for the "*List" endpoints:
 *   - generic sorting over a small set of column keys
 *   - generic filtering by per-column string values
 *   - paging
 *
 * The domain supplies:
 *   - the list of filterable column names
 *   - a `matches(t, key, value)` function
 *   - a `sortKey(t)` mapping from a column name to a comparable
 */
object ListQuerySupport:

  /** Column filter map from query params, restricted to `keys`. */
  def filterValues(keys:   List[String],
                    params: String => Option[String]
                  ): Map[String, String] =
    keys.flatMap(k => params(k).map(k -> _)).toMap

  /**
   * Apply a list of (column, direction) sort specs, right-to-left so that
   * earlier specs are primary. Unknown keys are silently ignored.
   */
  def applySort[T](rows: List[T],
                    sort: List[(String, String)],
                    orderingFor: String => Option[Ordering[T]]
                  ): List[T] =
    sort.foldRight(rows) { case ((key, dir), acc) =>
      orderingFor(key) match
        case Some(ord) =>
          val ordered = acc.sorted(using ord)
          if dir == "desc" then ordered.reverse else ordered
        case None => acc
    }

  /** Generic pagination: returns (slice, totalSize). */
  def paginate[T](rows: List[T], page: Int, size: Int): (List[T], Long) =
    (rows.slice(page * size, page * size + size), rows.size.toLong)

  /** Apply free-text search (substring, case-insensitive) and column filters. */
  def applyFilters[T](rows: List[T],
                       query: String,
                       filters: Map[String, String],
                       searchText: (T, String) => Boolean,
                       matches: (T, String, String) => Boolean
                     ): List[T] =
    val q = query.trim
    rows
      .filter(t => q.isEmpty || searchText(t, q.toLowerCase))
      .filter(t => filters.forall { case (k, v) => matches(t, k, v) })