package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
//import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.{Role, User, UserRight, UserRole}
import com.kabasoft.iws.domain.AppError.RepositoryError

import java.time.{Instant, LocalDateTime, ZoneId}

final case class UserRepositoryLive(postgres: Resource[Task, Session[Task]], repo: RoleRepository) extends UserRepository, MasterfileCRUD:

  import UserRepositorySQL._

  override def create(c: User): ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, insert, 1)
  override def create(list: List[User]):ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(User.encodeIt), insertAll(list.size), list.size)
  override def modify(model: User):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, User.encodeIt2, UPDATE, 1)
  override def modify(models: List[User]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, User.encodeIt2)
  def modifyPwd(model: User): ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, (model.userName, model.modelid, model.company), updatePwd, 1)
  def list(p: (Int, String)):ZIO[Any, RepositoryError, List[User]] = queryWithTx(postgres, p, ALL)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[User]] = for {
    users_ <- list(p)
    users <- setRoleAndRight(p._2, users_)
  } yield users

  private def setRoleAndRight(p: (String, List[User]) ):ZIO[Any, RepositoryError, List[User]] = for {
    roles <- repo.all(Role.MODEL_ID, p._1)
    user_rights <- repo.allRights(UserRight.MODEL_ID, p._1)
    user_roles <- repo.userRoles(UserRole.MODEL_ID, p._1)
    users_ = p._2
  }yield {
    val  rolesx: List[Role] = roles.map( r=>r.copy(rights = r.rights.:::(user_rights.filter(rt => rt.roleid == r.id))))
    val user_role = rolesx.filter(r=> user_roles.map(_.roleid).contains(r.id))
    val users  = users_.map(u => u.copy(roles = user_role, rights = u.roles.flatMap(_.rights)))
    users
  }

  override def getById(p: (Int, Int, String)):ZIO[Any, RepositoryError, User] = for {
    users_ <- queryWithTxUnique(postgres, p, BY_ID)
    users <- setRoleAndRight(p._3, List(users_))
  } yield users.headOption.getOrElse(User.dummy)

  override def getBy(ids: List[Int], modelid: Int, company: String):ZIO[Any, RepositoryError, List[User]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  override def getByUserName(p: (String, Int, String)):ZIO[Any, RepositoryError, User]= for {
      users_ <- queryWithTxUnique(postgres, p, BY_NAME)
      users <- setRoleAndRight(p._3, List(users_))
  } yield users.headOption.getOrElse(User.dummy)


  override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)


object UserRepositoryLive:

  val live: ZLayer[Resource[Task, Session[Task]] & RoleRepository, RepositoryError, UserRepository] =
    ZLayer.fromFunction(new UserRepositoryLive(_, _))

private[repository] object UserRepositorySQL:
  type TYPE = (Int, String, String, String, String, String, String, String, String, String, Int)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (int4 *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar  *: varchar *: varchar *: varchar *: int4)

  private[repository] def encodeIt(st: User): TYPE =
    (
      st.id,
      st.userName,
      st.firstName,
      st.lastName,
      st.hash,
      st.phone,
      st.email,
      st.department,
      st.menu,
      st.company,
      st.modelid
    )

  val mfDecoder: Decoder[User] = mfCodec.map:
      case (id, userName, firstName, lastName, hash, phone, email, department, menu, company, modelid) =>
          User(id, userName, firstName, lastName, hash, phone, email, department, menu, company, modelid)

  val mfEncoder: Encoder[User] = mfCodec.values.contramap(encodeIt)

  def base =
    sql""" SELECT id, user_name, first_name, last_name, hash, phone, email, department, menu, company, modelid
           FROM   users """

  def ALL_BY_ID(nr: Int): Query[(List[Int], Int, String), User] =
    sql"""SELECT id, user_name, first_name, last_name, hash, phone, email, department, menu, company, modelid
           FROM   users
           WHERE id  IN ${int4.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Int *: Int *: String *: EmptyTuple, User] =
    sql"""SELECT id, user_name, first_name, last_name, hash, phone, email, department, menu, company, modelid
           FROM   users
           WHERE id = $int4 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_NAME: Query[String *: Int *: String *: EmptyTuple, User] =
    sql"""SELECT id, user_name, first_name, last_name, hash, phone, email, department, menu, company, modelid
           FROM   users
           WHERE user_name = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, User] =
    sql"""SELECT id, user_name, first_name, last_name, hash, phone, email, department, menu, company, modelid
           FROM   users
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[User] = sql"""INSERT INTO User VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[TYPE]] =
    sql"INSERT INTO users VALUES ${mfCodec.values.list(n)}".command


    
  val UPDATE: Command[User.TYPE2] =
    sql"""UPDATE users
          SET first_name = $varchar, last_name = $varchar, phone = $varchar, email=$varchar, department=$varchar
          , menu=$varchar
          WHERE id=$int4 and modelid=$int4 and company= $varchar""".command
    
  val updatePwd: Command[(String, Int,  String)] =
    sql"""UPDATE  users
           SET  
            hash              = EXCLUDED.hash
            WHERE user_name = $varchar AND modelid = $int4 AND company = $varchar
          """.command
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM users WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command