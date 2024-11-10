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
import com.kabasoft.iws.domain.Bom
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{ Instant, LocalDateTime, ZoneId, ZoneOffset }

final case class BomRepositoryLive(postgres: Resource[Task, Session[Task]]) extends BomRepository, MasterfileCRUD:

  import BomRepositorySQL._

  override def create(c: Bom, flag: Boolean):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  override def create(list: List[Bom]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(encodeIt), insertAll(list.size), list.size)
  override def modify(model: Bom):ZIO[Any, RepositoryError, Int]= create(model, true)
  override def modify(models: List[Bom]):ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Bom]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Bom] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Bom]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, p, DELETE, 1)


object BomRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, BomRepository] =
    ZLayer.fromFunction(new BomRepositoryLive(_))

private[repository] object BomRepositorySQL:
  type TYPE=(String, String, BigDecimal, String, String, Int)
  private val mfCodec = (varchar *: varchar *: numeric(12, 2) *: varchar *: varchar *: int4)

  private[repository] def encodeIt(st: Bom):TYPE =
    (st.id, st.parent, st.quantity, st.description, st.company, st.modelid)

  val mfDecoder: Decoder[Bom] = mfCodec.map:
    case (id, parent, quantity, description, company, modelid) => Bom(id, parent, quantity.bigDecimal, description, company, modelid)
  
  val mfEncoder: Encoder[Bom] = mfCodec.values.contramap(encodeIt)

  def base =
    sql""" SELECT id, parent, quantity, description, company, modelid
           FROM   Bom """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Bom] =
    sql"""SELECT id, parent, quantity, description, company, modelid
           FROM   Bom
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Bom] =
    sql"""SELECT id, parent, quantity, description, company, modelid
           FROM   bom
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Bom] =
    sql"""SELECT id, parent, quantity, description, company, modelid
           FROM   bom
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Bom] = sql"""INSERT INTO Bom VALUES $mfEncoder""".command
  def insertAll(n:Int): Command[List[TYPE]] = sql"INSERT INTO bom VALUES ${mfCodec.values.list(n)}".command

  val upsert: Command[Bom] =
    sql"""INSERT INTO bom
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

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM bom WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
