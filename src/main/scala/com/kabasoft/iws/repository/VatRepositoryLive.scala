package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.{Task, ZIO, ZLayer}
import java.time.{Instant, LocalDateTime, ZoneId}
import com.kabasoft.iws.domain.Vat
import com.kabasoft.iws.domain.AppError.RepositoryError


import scala.math.BigDecimal

final class VatRepositoryLive(postgres: Resource[Task, Session[Task]]) extends VatRepository, MasterfileCRUD:

  import VatRepositorySQL._

  override def create(c: Vat):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Vat]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Vat.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Vat):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Vat.encodeIt2, UPDATE, 1)
  override def modify(models: List[Vat]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Vat.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Vat]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Vat] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Vat]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
    
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
 
object VatRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, VatRepository] =
    ZLayer.fromFunction(new VatRepositoryLive(_))

private[repository] object VatRepositorySQL:
    type TYPE = (String, String, String, BigDecimal, String, String, LocalDateTime, LocalDateTime, LocalDateTime, String, Int)
    def toInstant(localDateTime: LocalDateTime): Instant = 
       localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
    private val mfCodec =
    (varchar *: varchar *: varchar *: numeric(12, 2) *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4)
    val mfDecoder: Decoder[Vat] = mfCodec.map :
        case (id, name, description, percent, inputVatAccount, outputVatAccount, enterdate, changedate, postingdate, company, modelid) =>
             Vat(id, name, description, percent.bigDecimal, inputVatAccount, outputVatAccount, toInstant(enterdate), toInstant(changedate),
             toInstant(postingdate), company, modelid)
             
    val mfEncoder: Encoder[Vat] = mfCodec.values.contramap(Vat.encodeIt)

    def base =
    sql""" SELECT id, name, description, percent, input_vat_account, output_vat_account, enterdate, changedate,postingdate, company, modelid
         FROM   vat ORDER BY id ASC """

    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Vat] =
    sql"""
         SELECT id, name, description, percent, input_vat_account, output_vat_account, enterdate, changedate,postingdate, company, modelid
         FROM   vat
         WHERE id  IN (${varchar.list(nr)}) AND  modelid = $int4 AND company = $varchar
         ORDER BY id ASC """.query(mfDecoder)

    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Vat] =
    sql"""SELECT id, name, description, percent, input_vat_account, output_vat_account, enterdate, changedate,postingdate, company, modelid
         FROM   vat
         WHERE id = $varchar AND modelid = $int4 AND company = $varchar
         ORDER BY id ASC """.query(mfDecoder)

    val ALL: Query[Int *: String *: EmptyTuple, Vat] =
    sql"""SELECT id, name, description, percent, input_vat_account, output_vat_account, enterdate, changedate, postingdate, company, modelid
         FROM   vat
         WHERE  modelid = $int4 AND company = $varchar
         ORDER BY id ASC """.query(mfDecoder)

    val insert: Command[Vat] = sql"""INSERT INTO vat VALUES $mfEncoder""".command

    def insertAll(n: Int): Command[List[TYPE]] = sql"INSERT INTO vat VALUES ${mfCodec.values.list(n)}".command
  

    val UPDATE: Command[(String, String, BigDecimal, String, String, String, Int, String)] =
         sql"""UPDATE vat
            SET name = $varchar, description = $varchar, percent = $numeric, input_vat_account= $varchar
            , output_vat_account = $varchar
            WHERE id=$varchar and modelid=$int4 and company= $varchar""".command

    val DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM vat WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
