package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Permission
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case class PermissionRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PermissionRepository, MasterfileCRUD:

  import PermissionRepositorySQL.*

  override def create(c: Permission):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Permission]):ZIO[Any, RepositoryError, Int] =
     executeWithTx(postgres, list.map(Permission.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Permission):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Permission.encodeIt2, UPDATE, 1)
  override def modify(models: List[Permission]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Permission.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Permission]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Int, Int, String)): ZIO[Any, RepositoryError, Permission] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Int], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Permission]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  def delete(p: (Int, Int, String)): ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, p, DELETE, 1)

object PermissionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PermissionRepository] =
    ZLayer.fromFunction(new PermissionRepositoryLive(_))

private[repository] object PermissionRepositorySQL:

  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (int4 *:  varchar *: varchar  *: timestamp *: timestamp *: timestamp *: int4 *: varchar )

  val mfDecoder: Decoder[Permission] = mfCodec.map {
    case (id, name, description, enterdate, changedate, postingdate, modelid, company) =>
      Permission(
        id,
        name,
        description,
        toInstant(enterdate),
        toInstant(changedate),
        toInstant(postingdate),
        modelid,
        company)
  }

  val mfEncoder: Encoder[Permission] = mfCodec.values.contramap(Permission.encodeIt)

  def base =
    sql""" SELECT id, name, description, enterdate, changedate,postingdate, company, modelid
           FROM   permission """
  
  def ALL_BY_ID(nr: Int): Query[(List[Int], Int, String), Permission] =
    sql"""SELECT id, name, description, enterdate, changedate,postingdate, company, modelid
           FROM   permission
           WHERE id  IN ${int4.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Int *: Int *: String *: EmptyTuple, Permission] =
    sql"""SELECT id, name, description, enterdate, changedate, postingdate, modelid, company 
           FROM   permission
           WHERE id = $int4 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Permission] =
    sql"""SELECT id, name, description, enterdate, changedate,postingdate, modelid, company 
           FROM   permission
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Permission] = sql"""INSERT INTO permission VALUES $mfEncoder""".command

  def insertAll(n:Int): Command[List[Permission.TYPE]] =
    sql"INSERT INTO permission VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Permission.TYPE2] =
    sql"""UPDATE permission
          SET name = $varchar, description = $varchar
          WHERE id=$int4 and modelid=$int4 and company= $varchar""".command
    
  val upsert: Command[Permission] =
    sql"""INSERT INTO permission
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                     = EXCLUDED.id,
           name                   = EXCLUDED.name,
           description            = EXCLUDED.description,
            account               = EXCLUDED.parent,
            enterdate             = EXCLUDED.enterdate,
            changedate            = EXCLUDED.changedate,
            postingdate           = EXCLUDED.postingdate,
            company               = EXCLUDED.company,
            modelid               = EXCLUDED.modelid,
          """.command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"
  
  def DELETE: Command[(Int, Int, String)] =
    sql"DELETE FROM permission WHERE id = $int4 AND modelid = $int4 AND company = $varchar".command
