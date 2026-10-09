package com.kabasoft.iws.api

import zio._
import zio.http._

object HtmxEndpoint:

  /**
   * Standard GET handler for an HTMX form fragment:
   *   1. load the entity by (id, modelid, company)
   *   2. render the fragment (possibly fetching extra data)
   *
   * Any error becomes an inline HTML error fragment.
   */
  def loadForm[R, E, T](
                         id: String,
                         modelid: Int,
                         company: String,
                         load: (String, Int, String) => ZIO[R, E, T],
                         render: T => ZIO[R, E, String]
                       ): ZIO[R, Nothing, Response] =
    (for {
      entity <- load(id, modelid, company)
      html <- render(entity)
    } yield HtmxResponse.html(html))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))

  /**
   * Same as `loadForm`, but wraps the fragment in the full HTML page shell.
   * Use this for the page route that serves the wrapper.
   */
  def loadPage[R, E, T](
                         title: String,
                         id: String,
                         modelid: Int,
                         company: String,
                         load: (String, Int, String) => ZIO[R, E, T],
                         render: T => ZIO[R, E, String]
                       ): ZIO[R, Nothing, Response] =
    loadForm(id, modelid, company, load, (t: T) =>
      render(t).map(fragment => HtmxPage.render(title, fragment))
    )
  /**
   * Standard PUT handler for an HTMX form:
   *   1. parse the form body
   *   2. extract id / modelid / company from the hidden fields
   *   3. load the current entity
   *   4. apply the form fields via the `update` function
   *   5. save
   *   6. reload the fresh entity
   *   7. render the updated fragment via the `render` function
   *
   * All errors become an inline HTML error fragment, so the route
   * always returns a Response and the browser keeps the form on screen.
   */
  def saveForm[R, E, T](
                         req: Request,
                         load:   (String, Int, String) => ZIO[R, E, T],
                         update: (T, Map[String, String]) => T,
                         save:   T => ZIO[R, E, Int],
                         render: T => ZIO[R, E, String]
                       ): ZIO[R, Nothing, Response] =
    (for {
      body     <- req.body.asString
      params    = HtmxResponse.parseForm(body)
      id        = params.getOrElse("id", "")
      modelid   = params.get("modelid").flatMap(_.toIntOption).getOrElse(0)
      company   = params.getOrElse("company", "")
      existing <- load(id, modelid, company)
      updated   = update(existing, params)
      _        <- save(updated)
      fresh    <- load(id, modelid, company)
      html     <- render(fresh)
    } yield HtmxResponse.html(html))
      .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))