package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.ZStream
import zio.stream.interop.fs2z.*
import zio.{ Chunk, Task, UIO, ZIO, ZLayer }
import java.time.LocalDate
import com.kabasoft.iws.domain.Store
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{ Instant, LocalDateTime, ZoneId, ZoneOffset }

final case class StoreRepositoryLive(postgres: Resource[Task, Session[Task]]) extends StoreRepository, MasterfileCRUD:

  import StoreRepositorySQL._

  override def create(c: Store, flag: Boolean):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  override def create(list: List[Store]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(encodeIt), insertAll(list.size), list.size)
  override def modify(model: Store):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Store.encodeIt2, UPDATE, 1)
  override def modify(models: List[Store]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Store.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Store]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Store] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Store]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, p, DELETE, 1)
  
object StoreRepositoryLive:

  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, StoreRepository] =
    ZLayer.fromFunction(new StoreRepositoryLive(_))

private[repository] object StoreRepositorySQL:

  type TYPE = (String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4)

  private[repository] def encodeIt(st: Store): TYPE =
    (
      st.id,
      st.name,
      st.description,
      st.account,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.company,
      st.modelid
    )

  val mfDecoder: Decoder[Store] = mfCodec.map:
      case (id, name, description, account, enterdate, changedate, postingdate, company, modelid) =>
          Store(id, name, description, account, toInstant(enterdate), toInstant(changedate), toInstant(postingdate),
            company, modelid)


  val mfEncoder: Encoder[Store] = mfCodec.values.contramap(encodeIt)

  def base =
    sql""" SELECT id, name, description, account, enterdate, changedate, postingdate, company, modelid
           FROM   store """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Store] =
    sql"""SELECT id, name, description, account, enterdate, changedate, postingdate, company, modelid
           FROM   store
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Store] =
    sql"""SELECT id, name, description, account, enterdate, changedate, postingdate, company, modelid
           FROM   store
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Store] =
    sql"""SELECT id, name, description, account, enterdate, changedate, postingdate, company, modelid
           FROM   store
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Store] = sql"""INSERT INTO store VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[(String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)]] =
    sql"INSERT INTO store VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Store.TYPE2] =
    sql"""UPDATE store
          SET name = $varchar, description = $varchar, account = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
    
  val upsert: Command[Store] =
    sql"""INSERT INTO store
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                     = EXCLUDED.id,
           name                   = EXCLUDED.name,
           description            = EXCLUDED.description,
            account               = EXCLUDED.account,
            enterdate             = EXCLUDED.enterdate,
            changedate            = EXCLUDED.changedate,
            postingdate           = EXCLUDED.postingdate,
            company               = EXCLUDED.company,
            modelid               = EXCLUDED.modelid,
          """.command

  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM store WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
