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
import com.kabasoft.iws.domain.{Role, UserRight, UserRole}
import com.kabasoft.iws.domain.AppError.RepositoryError

import java.time.{Instant, LocalDateTime, ZoneId}

final case class RoleRepositoryLive(postgres: Resource[Task, Session[Task]]) extends RoleRepository, MasterfileCRUD:

  import RoleRepositorySQL._

  override def create(c: Role):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Role]):ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(Role.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Role):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, Role.encodeIt2, UPDATE, 1)
  override def modify(models: List[Role]):ZIO[Any, RepositoryError, Int]= executeBatchWithTxK(postgres, models, UPDATE, Role.encodeIt2)
  override def all(p: (Int, String)):ZIO[Any, RepositoryError,  List[Role]] = queryWithTx(postgres, p, ALL)
  //def allUserRoles(p: (Int, String)):ZIO[Any, RepositoryError,  List[UserRole]] = queryWithTx(postgres, p, ALL_USER_ROLE)
  override def allRights(p: (Int,  String)):ZIO[Any, RepositoryError, List[UserRight]] = queryWithTx(postgres, p, ALL_RIGHTS)
  override def userRights(p: (Int, Int, String)):ZIO[Any, RepositoryError, List[UserRight]] = queryWithTx(postgres, p, USER_RIGHTS)
  override def userRoles(p: (Int, String)):ZIO[Any, RepositoryError, List[UserRole]] = queryWithTx(postgres, p, USER_ROLE)

//  override def allUserRoles(p: (Int, Int, String)): ZIO[Any, RepositoryError, List[UserRole]] = for {
//    roles <- userRole((p._1, p._2, p._3))
//    userRights <- userRights((p._1, p._3))
//  } yield roles.map(c => c.copy(rights = userRights.filter(_.roleid == c.roleid)))
  
  override def getById(p: (Int, Int, String)):ZIO[Any, RepositoryError, Role] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Int], modelid: Int, company: String):ZIO[Any, RepositoryError, List[Role]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  def delete(p: (Int, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object RoleRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, RoleRepository] =
    ZLayer.fromFunction(new RoleRepositoryLive(_))

private[repository] object RoleRepositorySQL:
  
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (int4 *: varchar *: varchar  *: timestamp *: timestamp *: timestamp *: varchar *: int4)
    
  private val rightCodec = (int4 *: int4 *: varchar *: varchar *: int4)
  private val userRoleCodec = (int4 *: int4 *: varchar *: int4)
  
  val mfDecoder: Decoder[Role] = mfCodec.map:
    case (id, name, description, enterdate, changedate, postingdate, company, modelid) =>
      Role(id, name, description, toInstant(enterdate), toInstant(changedate), toInstant(postingdate), modelid, company)

  val userRoleDecoder: Decoder[UserRole] = userRoleCodec.map:
    case (userId, roleId,  company, modelId) => UserRole(userId, roleId, company, modelId)
      
  val rightDecoder: Decoder[UserRight] = rightCodec.map:
     case (moduleId, roleId, short,  company, modelId) =>UserRight(moduleId,  roleId, short, company, modelId)

  val mfEncoder: Encoder[Role] = mfCodec.values.contramap(Role.encodeIt)

  def base =
    sql""" SELECT id, name, description, enterdate, changedate, postingdate, company, modelid
           FROM   role """

  def ALL_BY_ID(nr: Int): Query[(List[Int], Int, String), Role] =
    sql"""SELECT id, name, description, enterdate, changedate, postingdate, company, modelid
           FROM   role
           WHERE id  IN ${int4.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Int *: Int *: String *: EmptyTuple, Role] =
    sql"""SELECT id, name, description, enterdate, changedate, postingdate, company, modelid
           FROM   role
           WHERE id = $int4 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Role] = sql"""
           SELECT id, name, description, enterdate, changedate, postingdate, company, modelid
           FROM   role
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL_RIGHTS: Query[Int *: String *: EmptyTuple, UserRight] =
    sql"""SELECT moduleid, roleid, short, company, modelid
           FROM   user_right
           WHERE  modelid = $int4 AND company = $varchar
           """.query(rightDecoder)
    
  val USER_RIGHTS: Query[Int *: Int *:String *: EmptyTuple, UserRight] =
      sql"""SELECT moduleid, roleid, short, company, modelid
           FROM   user_right
           WHERE roleid = $int4 AND modelid = $int4 AND company = $varchar
           """.query(rightDecoder)

  val USER_ROLE: Query[Int *:String *: EmptyTuple, UserRole] =
    sql"""SELECT userid, roleid, company, modelid
           FROM   user_role
           WHERE   modelid = $int4 AND company = $varchar
           """.query(userRoleDecoder)

  val ALL_USER_ROLE: Query[Int *: String *: EmptyTuple, UserRole] =
    sql"""SELECT userid, roleid, company, modelid
           FROM   user_role
           WHERE  modelid = $int4 AND company = $varchar
           """.query(userRoleDecoder)
  

  val insert: Command[Role] = sql"""INSERT INTO role VALUES $mfEncoder """.command
  def insertAll(n:Int): Command[List[Role.TYPE2]] = sql"INSERT INTO role VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Role.TYPE3] =
    sql"""UPDATE role
          SET name = $varchar, description = $varchar
          WHERE id=$int4 and modelid=$int4 and company= $varchar""".command
    
  val upsert: Command[Role] =
    sql"""
          INSERT INTO role
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                   = EXCLUDED.id,
           name                 = EXCLUDED.name,
           description          = EXCLUDED.description,
            enterdate            = EXCLUDED.enterdate,
            changedate            = EXCLUDED.changedate,
            postingdate            = EXCLUDED.postingdate,
            company              = EXCLUDED.company,
            modelid              = EXCLUDED.modelid,
          """.command

  def DELETE: Command[(Int, Int, String)] =
    sql"DELETE FROM role WHERE id = $int4 AND modelid = $int4 AND company = $varchar".command
