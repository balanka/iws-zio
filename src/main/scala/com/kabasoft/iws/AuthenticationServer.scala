package com.kabasoft.iws

import com.kabasoft.iws.api.Utils.{bearerAuthWithContext, jwtEncode}

import java.time.Clock
import zio._
import zio.http._


/**
 * This is an example to demonstrate bearer Authentication middleware. The
 * Server has 2 routes. The first one is for login, Upon a successful login, it
 * will return a jwt token for accessing protected routes. The second route is a
 * protected route that is accessible only if the request has a valid jwt token.
 * AuthenticationClient example can be used to makes requests to this server.
 */
object AuthenticationServer extends ZIOAppDefault {
  implicit val clock: Clock = Clock.systemUTC

  // Secret Authentication key
  val SECRET_KEY = "secretKey"
  
  def routes: Routes[Any, Response] =
    Routes(
      // A route that is accessible only via a jwt token
      Method.GET / "profile" / "me" -> handler { (_: Request) =>
        ZIO.serviceWith[String](name => Response.text(s"Welcome $name!"))
      } @@ bearerAuthWithContext,

      // A login route that is successful only if the password is the reverse of the username
      Method.GET / "login" ->
        handler { (request: Request) =>
          val form = request.body.asMultipartForm.orElseFail(Response.badRequest)
          for {
            username <- form
              .map(_.get("username"))
              .flatMap(ff => ZIO.fromOption(ff).orElseFail(Response.badRequest("Missing username field!")))
              .flatMap(ff => ZIO.fromOption(ff.stringValue).orElseFail(Response.badRequest("Missing username value!")))
            password <- form
              .map(_.get("password"))
              .flatMap(ff => ZIO.fromOption(ff).orElseFail(Response.badRequest("Missing password field!")))
              .flatMap(ff => ZIO.fromOption(ff.stringValue).orElseFail(Response.badRequest("Missing password value!")))
          } yield
            if (password.reverse.hashCode == username.hashCode)
              //Response.text(jwtEncode(username, 15*24*60*60L))
              Response.text(jwtEncode(username, 15*24*60*60L))
            else
              Response.unauthorized("400:Invalid username or password.")
        },
    ) @@ Middleware.debug

  override val run = Server.serve(routes).provide(Server.default)
}