package com.kabasoft.iws.repository

import cats._
import cats.effect.Resource
import cats.syntax.all._
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Room
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.{Task, ZIO, ZLayer}
import java.time.{Instant, LocalDateTime, ZoneId}

final case class RoomRepositoryLive(postgres: Resource[Task, Session[Task]]) extends RoomRepository, MasterfileCRUD:
    import RoomRepositorySQL.*

    override def create(c: Room): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
    override def create(list: List[Room]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Room.encodeIt), insertAll(list.size), list.size)
    override def modify(model: Room):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Room.encodeIt2, UPDATE, 1)
    override def modify(models: List[Room]):ZIO[Any, RepositoryError, Int]= executeBatchWithTxK(postgres, models, UPDATE, Room.encodeIt2)
    override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Room]] = queryWithTx(postgres, p, ALL)
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Room] = queryWithTxUnique(postgres, p, BY_ID)
    override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Room]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    override def getByParent(parent: String, modelid: Int, company: String): ZIO[Any, RepositoryError, List[Room]] =
      queryWithTx(postgres, (parent, modelid, company), BY_PARENT)

    override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object RoomRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, RoomRepository] =
    ZLayer.fromFunction(new RoomRepositoryLive(_))

private[repository] object RoomRepositorySQL:
    private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
      localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

    private[repository] val mfCodec =
    (varchar *: varchar *: varchar *: varchar*: timestamp *: timestamp *: timestamp  *: int4 *: numeric(12,2) *: varchar *:  int4 )
  
    val mfDecoder: Decoder[Room] = mfCodec.map:
      case (id, name, description, parent, changedate, enterdate, postingdate, kind, area, company, modelid) =>
        Room(id, name, description, parent, toInstant(enterdate), toInstant(changedate), toInstant(postingdate)
          ,  kind, area.bigDecimal, company, modelid)

    val mfEncoder: Encoder[Room] = mfCodec.values.contramap(Room.encodeIt)

    val base =
     sql""" SELECT id, name, description, parent, enterdate, changedate, postingdate, kind, area, company, modelid
             FROM   room ORDER BY id ASC"""


    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Room] =
      sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, kind, area, company, modelid
         FROM   room
         WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
         ORDER BY id ASC""".query(mfDecoder)

    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Room] =
      sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, kind, area, company, modelid
         FROM   room
         WHERE id = $varchar AND modelid = $int4 AND company = $varchar
         ORDER BY id ASC""".query(mfDecoder)
         
    val BY_PARENT: Query[String *: Int *: String *: EmptyTuple, Room] =
      sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, kind, area, company, modelid
         FROM   room
         WHERE parent = $varchar AND modelid = $int4 AND company = $varchar
         ORDER BY id ASC""".query(mfDecoder)
    
    val ALL: Query[Int *: String *: EmptyTuple, Room] =
      sql"""SELECT id, name, description, parent, enterdate, changedate,postingdate, kind, area, company, modelid
         FROM   room
         WHERE  modelid = $int4 AND company = $varchar
         ORDER BY id ASC""".query(mfDecoder)

    val insert: Command[Room] =
      sql"""INSERT INTO room (id, name, description, parent, enterdate,changedate,postingdate, kind, area, company, modelid )
         VALUES $mfEncoder""".command

    def insertAll(n: Int): Command[List[Room.TYPE]] =
      sql"""INSERT INTO
         room (id, name, description, parent, enterdate, changedate,  postingdate, kind, area, company, modelid )
         VALUES ${mfCodec.values.list(n)}""".command

    val UPDATE: Command[Room.TYPE2] =
      sql"""UPDATE room SET name = $varchar, description = $varchar, parent = $varchar, kind =$int4, area = $numeric
          WHERE id=$varchar and company= $varchar and modelid=$int4""".command

    def DELETE: Command[(String, Int, String)] =
      sql"DELETE FROM room WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command

    def DELETE_ALL(nr: Int): Command[(List[String], Int, String)] =
      sql"DELETE FROM room WHERE id IN ${varchar.list(nr)} AND modelid = $int4 AND company = $varchar".command