package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Module
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case class ModuleRepositoryLive(postgres: Resource[Task, Session[Task]]) extends ModuleRepository, MasterfileCRUD:

  import ModuleRepositorySQL.*

  override def create(c: Module ):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Module]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Module.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Module):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Module.encodeIt2, UPDATE, 1)
  override def modify(models: List[Module]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Module.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Module]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Module] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Module]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object ModuleRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, ModuleRepository] =
    ZLayer.fromFunction(new ModuleRepositoryLive(_))

private[repository] object ModuleRepositorySQL:
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *:  varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4)

  val mfDecoder: Decoder[Module] = mfCodec.map:
    case (id, name, description, path, parent, enterdate, changedate, postingdate, modelid, company) =>
      Module(id, name, description, path, parent, toInstant(enterdate), toInstant(changedate), toInstant(postingdate),
        company, modelid)
  
  val mfEncoder: Encoder[Module] = mfCodec.values.contramap(Module.encodeIt)
  
  def base =
    sql""" SELECT id, name, description, path, parent, enterdate, changedate,postingdate, company, modelid
           FROM   module """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Module] =
    sql""" SELECT id, name, description, path, parent, enterdate, changedate,postingdate, company, modelid
           FROM   module
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Module] =
    sql"""SELECT id, name, description, path, parent, enterdate, changedate,postingdate, company, modelid
           FROM   module
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Module] =
    sql"""SELECT id, name, description, path, parent, enterdate, changedate,postingdate, company, modelid
           FROM   module
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Module] = sql"""INSERT INTO module VALUES $mfEncoder""".command
  def insertAll(n:Int): Command[List[Module.TYPE]] = sql"INSERT INTO module VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Module.TYPE2] =
    sql"""UPDATE module
          SET name = $varchar, description = $varchar, path = $varchar, parent = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
    
  val upsert: Command[Module] =
    sql"""INSERT INTO module
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                   = EXCLUDED.id,
           name                 = EXCLUDED.name,
           description          = EXCLUDED.description,
            path                 = EXCLUDED.path,
            parent                 = EXCLUDED.parent,
            enterdate            = EXCLUDED.enterdate,
            changedate            = EXCLUDED.changedate,
            postingdate            = EXCLUDED.postingdate,
            company              = EXCLUDED.company,
            modelid              = EXCLUDED.modelid,
          """.command

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM module WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
