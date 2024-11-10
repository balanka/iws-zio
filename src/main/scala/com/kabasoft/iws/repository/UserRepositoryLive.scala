package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import com.kabasoft.iws.domain.{Role, User}
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{Instant, LocalDateTime, ZoneId}

final case class UserRepositoryLive(postgres: Resource[Task, Session[Task]], repo: RoleRepository) extends UserRepository, MasterfileCRUD:

  import UserRepositorySQL._

  override def create(c: User, flag: Boolean): ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  override def create(list: List[User]):ZIO[Any, RepositoryError, Int] =executeWithTx(postgres, list.map(User.encodeIt), insertAll(list.size), list.size)
  override def modify(model: User):ZIO[Any, RepositoryError, Int] = create(model, true)
  override def modify(models: List[User]):ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
  def modifyPwd(model: User): ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, (model.userName, model.modelid, model.company), updatePwd, 1)
  def list(p: (Int, String)):ZIO[Any, RepositoryError, List[User]] = queryWithTx(postgres, p, ALL)

  override def all(p: (Int, String)):ZIO[Any, RepositoryError, List[User]] = for {
    users_ <- list(p)
    user_roles <- repo.listUserRoles(p)
    user_rights <- repo.userRights(p)
  }yield {
    val  rolesx: List[Role] = user_roles.map( r=>r.copy(rights = user_rights.filter(rt => rt.roleid == r.id)))
    val rolesN = rolesx.filter(r=> user_roles.filter(ur=>(ur.id == r.id)).map(ur=>ur.id).contains(r.id))
    val users  = users_.map(u => u.copy(roles = rolesN)).map(u=>  u.copy(rights = u.roles.flatMap(_.rights)))
    users
  }

  override def getById(p: (Int, Int, String)):ZIO[Any, RepositoryError, User] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Int], modelid: Int, company: String):ZIO[Any, RepositoryError, List[User]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  override def getByUserName(p: (String, Int, String)):ZIO[Any, RepositoryError, User]= queryWithTxUnique(postgres, p, BY_NAME)
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

  val upsert: Command[User] =
    sql"""INSERT INTO users
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
            user_name               = EXCLUDED.user_name,
            first_name              = EXCLUDED.first_name,
            last_name               = EXCLUDED.last_name,
            phone                   = EXCLUDED.phone,
            email                   = EXCLUDED.email,
            department              = EXCLUDED.department,
            menu                    = EXCLUDED.menu
            company                    = EXCLUDED.company
            modelid                    = EXCLUDED.modelid
          """.command
    
  val updatePwd: Command[(String, Int,  String)] =
    sql"""UPDATE  users
           SET  
            hash              = EXCLUDED.hash
            WHERE user_name = $varchar AND modelid = $int4 AND company = $varchar
          """.command

  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM users WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command