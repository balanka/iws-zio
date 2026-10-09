package com.kabasoft.iws.api

import com.kabasoft.iws.domain.{ModelId, User}
import com.kabasoft.iws.repository.UserRepository
import zio._
import zio.http._

object UserHtmlEndpoint:

  type Repos = UserRepository

  private def blank(company: String): User =
    User(
      id = -1, userName = "", firstName = "", lastName = "",
      hash = "", phone = "", email = "", department = "", menu = "",
      company = company, modelid = ModelId.USER.modelid,
      roles = Nil, rights = Nil, modules = Nil)

  private def renderUser(u: User, mode: String = "view")
  : ZIO[Any, Nothing, String] = ZIO.succeed(UserFormView.render(u, mode))

  private def applyParams(existing: User, params: Map[String, String]): User =
    existing.copy(
      userName   = params.getOrElse("userName", existing.userName),
      firstName  = params.getOrElse("firstName", existing.firstName),
      lastName   = params.getOrElse("lastName", existing.lastName),
      email      = params.getOrElse("email", existing.email),
      phone      = params.getOrElse("phone", existing.phone),
      department = params.getOrElse("department", existing.department),
      menu       = params.getOrElse("menu", existing.menu))

  private val listGet: Route[UserRepository, Response] =
    Method.GET / "html" / "user-list" / string("company") ->
      handler { (company: String, req: Request) =>
        val page  = req.queryParam("page").flatMap(_.toIntOption).getOrElse(0)
        val query = req.queryParam("q").getOrElse("")
        val size  = req.queryParam("size").flatMap(_.toIntOption)
          .filter(HtmxTable.pageSizes.contains).getOrElse(20)
        val sort  = req.queryParam("sort").map(HtmxTable.Page.parseSort)
          .filter(_.nonEmpty).getOrElse(HtmxTable.Page.defaultSort)
        UserRepository.all((ModelId.USER.modelid, company)).map { all =>
          val filtered =
            if query.isEmpty then all
            else all.filter(u => u.userName.toLowerCase.contains(query.toLowerCase) ||
              u.lastName.toLowerCase.contains(query.toLowerCase))
          val sorted = filtered.sortWith { (x, y) =>
            val cmp = sort.iterator.map { case (key, dir) =>
              val c = key match
                case "userName"  => x.userName.trim.compareTo(y.userName.trim)
                case "firstName" => x.firstName.trim.compareTo(y.firstName.trim)
                case "lastName"  => x.lastName.trim.compareTo(y.lastName.trim)
                case "email"     => x.email.trim.compareTo(y.email.trim)
                case _           => x.id.compareTo(y.id)
              if dir == "desc" then -c else c
            }.find(_ != 0).getOrElse(0)
            cmp < 0
          }
          val slice = sorted.slice(page * size, page * size + size)
          HtmxResponse.html(
            UserListView.render(
              HtmxTable.Page(slice, page, size, sorted.size.toLong, sort),
              company, query))
        }.catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formNew: Route[Any, Response] =
    Method.GET / "html" / "user" / "new" / int("modelid") / string("company") ->
      handler { (modelid: Int, company: String, _: Request) =>
        ZIO.succeed(HtmxResponse.html(
          UserFormView.render(blank(company), mode = "create")))
      }

  private val formGet: Route[Repos, Response] =
    Method.GET / "html" / "user" / int("id") / int("modelid") / string("company") ->
      handler { (id: Int, modelid: Int, company: String, _: Request) =>
        (for {
          u    <- UserRepository.getById((id, modelid, company))
          html <- renderUser(u)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  private val formPut: Route[Repos, Response] =
    Method.PUT / "html" / "user" ->
      handler { (req: Request) =>
        (for {
          body    <- req.body.asString
          params   = HtmxResponse.parseForm(body)
          mode     = params.getOrElse("mode", "view")
          id       = params.get("id").flatMap(_.toIntOption).getOrElse(-1)
          modelid  = params.get("modelid").flatMap(_.toIntOption).getOrElse(ModelId.USER.modelid)
          company  = params.getOrElse("company", "")
          existing <- if mode == "create" then ZIO.succeed(blank(company))
          else UserRepository.getById((id, modelid, company))
          toSave    = applyParams(existing, params)
          _        <- if mode == "create" then UserRepository.create(toSave).unit
          else UserRepository.modify(toSave).unit
          fresh    <- UserRepository.getById((toSave.id, modelid, company))
          html     <- renderUser(fresh)
        } yield HtmxResponse.html(html))
          .catchAll(err => ZIO.succeed(HtmxResponse.errorHtml(err.toString)))
      }

  val routes: Routes[Repos, Response] =
    Routes(listGet, formNew, formGet, formPut) @@ Middleware.debug