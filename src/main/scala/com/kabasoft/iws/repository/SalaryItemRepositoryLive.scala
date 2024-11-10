package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.SalaryItem
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case class SalaryItemRepositoryLive(postgres: Resource[Task, Session[Task]]) extends SalaryItemRepository, MasterfileCRUD:

  import SalaryItemRepositorySQL.*

  override def create(c: SalaryItem, flag: Boolean):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, if (flag) upsert else insert, 1)

  override def create(list: List[SalaryItem]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(SalaryItem.encodeIt), insertAll(list.size), list.size)
  
  override def modify(model: SalaryItem):ZIO[Any, RepositoryError, Int]= create(model, true)

  override def modify(models: List[SalaryItem]):ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.size)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[SalaryItem]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, SalaryItem] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[SalaryItem]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object SalaryItemRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, SalaryItemRepository] =
    ZLayer.fromFunction(new SalaryItemRepositoryLive(_))

private[repository] object SalaryItemRepositorySQL:
  
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: varchar *: numeric(12,2) *: numeric(12,2) *: timestamp *: timestamp *: timestamp *: varchar *: int4)

  val mfDecoder: Decoder[SalaryItem] = mfCodec.map:
    case (id, name, description, account, amount, percentage, enterdate, changedate, postingdate, company, modelid) =>
      SalaryItem(
        id,
        name,
        description,
        account,
        amount.bigDecimal,
        percentage.bigDecimal,
        toInstant(enterdate),
        toInstant(changedate),
        toInstant(postingdate),
        modelid,
        company
      )

  val mfEncoder: Encoder[SalaryItem] = mfCodec.values.contramap(SalaryItem.encodeIt)
  
  def base =
    sql""" SELECT id, name, description, account, amount, percentage, enterdate, changedate, postingdate, company, modelid
           FROM   salary_item """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), SalaryItem] =
    sql"""SELECT id, name, description, account, amount, percentage, enterdate, changedate, postingdate, company, modelid
           FROM   salary_item
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, SalaryItem] =
    sql""" SELECT id, name, description, account, amount, percentage, enterdate, changedate, postingdate, company, modelid
           FROM   salary_item
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, SalaryItem] =
    sql"""SELECT id, name, description, account, amount, percentage, enterdate, changedate, postingdate, company, modelid
           FROM   salary_item
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[SalaryItem] = sql"""INSERT INTO store VALUES $mfEncoder """.command
  def insertAll(n:Int): Command[List[SalaryItem.TYPE]] = sql"INSERT INTO salary_item VALUES ${mfCodec.values.list(n)}".command

    

  val upsert: Command[SalaryItem] =
    sql"""INSERT INTO salary_item
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                    = EXCLUDED.id,
           name                  = EXCLUDED.name,
           description           = EXCLUDED.description,
            account              = EXCLUDED.account,
            amount               = EXCLUDED.amount,
            percentage           = EXCLUDED.percentage,
            company              = EXCLUDED.company,
            modelid              = EXCLUDED.modelid,
          """.command

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM salary_item WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
