package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.{Task, ZIO, ZLayer}
import zio.prelude.FlipOps
import com.kabasoft.iws.domain.Partner
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{Instant, LocalDateTime, ZoneId}

final case class PartnerRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PartnerRepository, MasterfileCRUD:

  import PartnerRepositorySQL._
  
  override def create(c: Partner): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Partner]): ZIO[Any, RepositoryError, Int] = 
                         executeWithTx(postgres, list.map(Partner.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Partner): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Partner.encodeIt2, UPDATE, 1)
  override def modify(models: List[Partner]): ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Partner.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Partner]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Partner] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Partner]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  override def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)
    //executeWithTx[ List[(String, Int, String)]](postgres, p, DELETE_ALL(p.size), p.size)

object PartnerRepositoryLive:
 
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PartnerRepository] =
    ZLayer.fromFunction(new PartnerRepositoryLive(_))

private[repository] object PartnerRepositorySQL:
  
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
 
  private val mfCodec =
    (varchar(50) *: varchar(255) *: varchar(255) *: varchar  *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: int4 *:timestamp *: timestamp *: timestamp )
  
  val mfDecoder: Decoder[Partner] = mfCodec.map:
    case (id, name, description, street, zip, city, state, country, phone, email, company, modelid, enterdate, changedate, postingdate) =>
      Partner(id, name, description, street, zip, city, state, country, phone, email, company, modelid 
        , toInstant(enterdate), toInstant(changedate), toInstant(postingdate)
       )
  

  val mfEncoder: Encoder[Partner] = mfCodec.values.contramap(Partner.encodeIt) 

  val base =
    sql""" SELECT id, name, description, , street, zip, city, state, country, phone, email, enterdate, changedate,postingdate, company, modelid
           FROM   masterfile ORDER BY id ASC"""

  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Partner] = 
    sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, enterdate, changedate,postingdate, company, modelid
           FROM   partner
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Partner] =
    sql"""SELECT id, name, description,  street, zip, city, state, country, phone, email, enterdate, changedate,postingdate, company, modelid
           FROM   partner
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Partner] =
    sql"""SELECT id, name, description, street, zip, city, state, country, phone, email, enterdate, changedate,postingdate, company, modelid
           FROM   partner
           WHERE  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)
  
  val insert: Command[Partner] =
    sql"""INSERT INTO partner (id, name, description, street, zip, city, state, country, phone, email, enterdate,changedate,postingdate, company, modelid )
         VALUES $mfEncoder""".command
    
  def insertAll(n:Int):Command[List[Partner.TYPE]]= sql"""INSERT INTO 
           partner (id, name, description, street, zip, city, state, country, phone, email, enterdate,changedate,postingdate, company, modelid ) 
           VALUES ${mfCodec.values.list(n)}""".command
  
  val UPDATE: Command[Partner.TYPE2] =
    sql"""UPDATE partner
          SET name = $varchar, description = $varchar, street= $varchar, zip= $varchar, city= $varchar
            , state= $varchar, country= $varchar, phone= $varchar, email= $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM partner WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command

  def DELETE_ALL(nr:Int): Command[(List[String], Int, String)] =
    sql"DELETE FROM partner WHERE id IN ${varchar.list(nr)} AND modelid = $int4 AND company = $varchar".command