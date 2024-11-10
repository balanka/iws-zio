package com.kabasoft.iws.repository
import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.Masterfile
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{ Instant, LocalDateTime, ZoneId }

final case class MasterfileRepositoryLive(postgres: Resource[Task, Session[Task]]) extends MasterfileRepository, MasterfileCRUD:

  import MasterfileRepositorySQL._
  
  override def create(c: Masterfile, flag: Boolean): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  override def create(list: List[Masterfile]): ZIO[Any, RepositoryError, Int] = 
                         executeWithTx(postgres, list.map(Masterfile.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Masterfile): ZIO[Any, RepositoryError, Int] = create(model, true)
  override def modify(models: List[Masterfile]): ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Masterfile]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Masterfile] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Masterfile]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

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
           FROM   masterfile """

  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Masterfile] = 
    sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Masterfile] =
    sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Masterfile] =
    sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Masterfile] = sql"INSERT INTO masterfile VALUES $mfEncoder".command
  def insertAll(n:Int):Command[List[TYPE]]= sql"INSERT INTO masterfile VALUES ${mfCodec.values.list(n)}".command
  val upsert: Command[Masterfile] =
    sql"""INSERT INTO masterfile
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                     = EXCLUDED.id,
           name                   = EXCLUDED.name,
           description            = EXCLUDED.description,
            parent                = EXCLUDED.parent,
            enterdate             = EXCLUDED.enterdate,
            changedate            = EXCLUDED.changedate,
            postingdate           = EXCLUDED.postingdate,
            company               = EXCLUDED.company,
            modelid               = EXCLUDED.modelid,
          """.command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM masterfile WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
