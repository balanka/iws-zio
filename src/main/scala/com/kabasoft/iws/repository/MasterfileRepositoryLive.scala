package com.kabasoft.iws.repository
import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.{Task, ZIO, ZLayer}
import zio.prelude.FlipOps
import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{Instant, LocalDateTime, ZoneId}

final case class MasterfileRepositoryLive(postgres: Resource[Task, Session[Task]]) extends MasterfileRepository, MasterfileCRUD:

  import MasterfileRepositorySQL._
  
  override def create(c: Masterfile): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Masterfile]): ZIO[Any, RepositoryError, Int] = 
                         executeWithTx(postgres, list.map(Masterfile.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Masterfile): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Masterfile.encodeIt2, UPDATE, 1)
  override def modify(models: List[Masterfile]): ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Masterfile.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Masterfile]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Masterfile] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Masterfile]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  override def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)
    //executeWithTx[ List[(String, Int, String)]](postgres, p, DELETE_ALL(p.size), p.size)

object MasterfileRepositoryLive:
 
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, MasterfileRepository] =
    ZLayer.fromFunction(new MasterfileRepositoryLive(_))

private[repository] object MasterfileRepositorySQL:

  type TYPE = (String, String, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
 
  private val mfCodec =
    (varchar(50) *: varchar(255) *: varchar(255) *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4)
  
  val mfDecoder: Decoder[Masterfile] = mfCodec.map:
    case (id, name, description, parent, enterdate, changedate, postingdate, company, modelid) =>
      Masterfile(id, name, description, parent, toInstant(enterdate), toInstant(changedate), toInstant(postingdate)
        , modelid, company)
  

  val mfEncoder: Encoder[Masterfile] = mfCodec.values.contramap(Masterfile.encodeIt) 

  val base =
    sql""" SELECT id, name, description, parent, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile ORDER BY id ASC"""

  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Masterfile] = 
    sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Masterfile] =
    sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Masterfile] =
    sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile
           WHERE  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val insert: Command[Masterfile] =
    sql"""INSERT INTO masterfile (id, name, description, parent, enterdate,changedate,postingdate, company, modelid )
         VALUES $mfEncoder""".command
    
  def insertAll(n:Int):Command[List[TYPE]]= sql"""INSERT INTO 
           masterfile (id, name, description, parent, enterdate,changedate,postingdate, company, modelid ) 
           VALUES ${mfCodec.values.list(n)}""".command
  
  val UPDATE: Command[Masterfile.TYPE2] =
    sql"""UPDATE masterfile
          SET name = $varchar, description = $varchar, parent = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM masterfile WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command

  def DELETE_ALL(nr:Int): Command[(List[String], Int, String)] =
    sql"DELETE FROM masterfile WHERE id IN ${varchar.list(nr)} AND modelid = $int4 AND company = $varchar".command
