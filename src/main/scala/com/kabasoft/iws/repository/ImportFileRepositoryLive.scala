package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.ImportFile
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{ Instant, LocalDateTime, ZoneId }

final case class ImportFileRepositoryLive(postgres: Resource[Task, Session[Task]]) extends ImportFileRepository, MasterfileCRUD:

  import ImportFileRepositorySQL._

  override def create(c: ImportFile):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[ImportFile]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(ImportFile.encodeIt), insertAll(list.size), list.size)
  override def modify(model: ImportFile): ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, ImportFile.encodeIt2, UPDATE, 1)
  override def modify(models: List[ImportFile]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, ImportFile.encodeIt2)
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
    sql""" SELECT id, name, description, extension, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file """
  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), ImportFile] =
    sql"""SELECT id, name, description, extension, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, ImportFile] =
    sql"""SELECT id, name, description, extension, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, ImportFile] =
    sql"""SELECT id, name, description, extension, enterdate, changedate,postingdate, company, modelid
           FROM   bankstatement_file
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[ImportFile] = sql"""INSERT INTO bankstatement_file VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[ImportFile.TYPE]] =
    sql"INSERT INTO bankstatement_file VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[ImportFile.TYPE2] =
    sql"""UPDATE bankstatement_file
          SET name = $varchar, description = $varchar, extension = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM bankstatement_file WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
