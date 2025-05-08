package com.kabasoft.iws.repository
import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.Fmodule
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{ Instant, LocalDateTime, ZoneId }

final case class FModuleRepositoryLive(postgres: Resource[Task, Session[Task]]) extends FModuleRepository, MasterfileCRUD:

  import FModuleRepositorySQL.*

  override def create(c: Fmodule):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Fmodule]):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, list.map(encodeIt), insertAll(list.size), list.size)
  override def modify(model: Fmodule):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, Fmodule.encodeIt2, UPDATE, 1)
  override def modify(models: List[Fmodule]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Fmodule.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Fmodule]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Int, Int, String)): ZIO[Any, RepositoryError, Fmodule] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Int], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Fmodule]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    
  def delete(p: (Int, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object FModuleRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, FModuleRepository] =
    ZLayer.fromFunction(new FModuleRepositoryLive(_))

private[repository] object FModuleRepositorySQL:
  type TYPE = (Int, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Boolean, String, Int, String)
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (int4 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: bool  *: varchar *: int4 *: varchar)
  private[repository] def encodeIt(st: Fmodule): TYPE =
    (st.id,
      st.name,
      st.description,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.account,
      st.isDebit,
      st.parent,
      st.modelid,
      st.company
    )
  val mfDecoder: Decoder[Fmodule] = mfCodec.map :
    case (id, name, description, enterdate, changedate, postingdate, account, isDebit,  parent, modelid, company) =>
      Fmodule(
        id,
        name,
        description,
        toInstant(enterdate),
        toInstant(changedate),
        toInstant(postingdate),
        account,
        isDebit,
        parent,
        modelid,
        company)

  val mfEncoder: Encoder[Fmodule] = mfCodec.values.contramap((st: Fmodule) =>
    (
      st.id,
      st.name,
      st.description,
      st.enterdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.changedate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.postingdate.atZone(ZoneId.of("Europe/Paris")).toLocalDateTime,
      st.account,
      st.isDebit, 
      st.parent,
      st.modelid,
      st.company,
      
    )
  )

  def base =
    sql""" SELECT id, name, description, enterdate, changedate,postingdate, account, is_debit, parent, modelid, company
           FROM   fmodule ORDER BY id ASC"""

  def ALL_BY_ID(nr: Int): Query[(List[Int], Int, String), Fmodule] =
    sql"""SELECT id, name, description, enterdate, changedate,postingdate, account, is_debit, parent, modelid, company
           FROM   fmodule
           WHERE id  IN ${int4.list(nr)} AND  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val BY_ID: Query[Int *: Int *: String *: EmptyTuple, Fmodule] =
    sql"""SELECT id, name, description, enterdate, changedate,postingdate, account, is_debit, parent, modelid, company
           FROM   fmodule
           WHERE id = $int4 AND modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Fmodule] =
    sql"""SELECT id, name, description, enterdate, changedate, postingdate, account, is_debit, parent, modelid, company
           FROM   fmodule
           WHERE  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val insert: Command[Fmodule] = sql"""INSERT INTO fmodule VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[TYPE]]= sql"INSERT INTO fmodule VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Fmodule.TYPE2] =
    sql"""UPDATE fmodule
          SET name = $varchar, description = $varchar, account = $varchar, is_debit=$bool, parent=$varchar
          WHERE id=$int4 and modelid=$int4 and company= $varchar""".command
  
  def DELETE: Command[(Int, Int, String)] =
    sql"DELETE FROM fmodule WHERE id = $int4 AND modelid = $int4 AND company = $varchar".command
