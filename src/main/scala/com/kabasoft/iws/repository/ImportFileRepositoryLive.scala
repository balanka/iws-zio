package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{ Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.ImportFile
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{ Instant, LocalDateTime, ZoneId }

final case class ImportFileRepositoryLive(postgres: Resource[Task, Session[Task]]) extends ImportFileRepository, MasterfileCRUD:

  import ImportFileRepositorySQL._

  override def create(c: ImportFile, flag: Boolean):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  override def create(list: List[ImportFile]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(ImportFile.encodeIt), insertAll(list.size), list.size)
  override def modify(model: ImportFile): ZIO[Any, RepositoryError, Int]= create(model, true)
  override def modify(models: List[ImportFile]):ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[ImportFile]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, ImportFile] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[ImportFile]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  
object ImportFileRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, ImportFileRepository] =
    ZLayer.fromFunction(new ImportFileRepositoryLive(_))

private[repository] object ImportFileRepositorySQL:

  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4)

  val mfDecoder: Decoder[ImportFile] = mfCodec.map:
    case (id, name, description, extension, enterdate, changedate, postingdate, company, modelid) =>
      ImportFile(id, name, description, extension, toInstant(enterdate), toInstant(changedate), toInstant(postingdate)
        ,modelid, company)

  val mfEncoder: Encoder[ImportFile] = mfCodec.values.contramap(ImportFile.encodeIt) 

  def base =
    sql""" SELECT id, name, description, account, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file """
  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), ImportFile] =
    sql"""SELECT id, name, description, account, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, ImportFile] =
    sql"""SELECT id, name, description, account, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, ImportFile] =
    sql"""SELECT id, name, description, extension, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[ImportFile] = sql"""INSERT INTO bankstatement_file VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[(String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)]] =
    sql"INSERT INTO bankstatement_file VALUES ${mfCodec.values.list(n)}".command

  val upsert: Command[ImportFile] =
    sql"""INSERT INTO bankstatement_file
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
            id                     = EXCLUDED.id,
            name                   = EXCLUDED.name,
            description            = EXCLUDED.description,
            extension              = EXCLUDED.extension,
            enterdate              = EXCLUDED.enterdate,
            changedate             = EXCLUDED.changedate,
            postingdate            = EXCLUDED.postingdate,
            company                = EXCLUDED.company,
            modelid                = EXCLUDED.modelid,
          """.command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM bankstatement_file WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
