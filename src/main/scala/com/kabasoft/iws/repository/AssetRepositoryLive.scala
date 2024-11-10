package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Asset
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import java.time.{Instant, LocalDateTime, ZoneId}

final case class AssetRepositoryLive(postgres: Resource[Task, Session[Task]]) extends AssetRepository, MasterfileCRUD:
    import AssetRepositorySQL.*

    override def create(c: Asset, flag: Boolean): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, if (flag) upsert else insert, 1)
  
    override def create(list: List[Asset]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Asset.encodeIt), insertAll(list.size), list.size)
    override def modify(model: Asset):ZIO[Any, RepositoryError, Int] = create(model, true)
    override def modify(models: List[Asset]):ZIO[Any, RepositoryError, Int]= models.map(modify).flip.map(_.sum)
    override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Asset]] = queryWithTx(postgres, p, ALL)
    override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Asset] = queryWithTxUnique(postgres, p, BY_ID)
    override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Asset]] =
      queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

    override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

object AssetRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, AssetRepository] =
    ZLayer.fromFunction(new AssetRepositoryLive(_))

private[repository] object AssetRepositorySQL:
    private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
      localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
    private[repository] val mfCodec =
    (varchar *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4 *: varchar *: varchar *: int4 *: numeric(12,2) *: numeric(12,2) *: int4 *: numeric(12,2) *: int4 *: varchar )
  
    val mfDecoder: Decoder[Asset] = mfCodec.map:
      case (id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount, dep_method, amount, rate, life_span, scrap_value, frequency, currency) =>
        Asset( id, name, description, toInstant(changedate), toInstant(enterdate), toInstant(postingdate), company
          , modelid, account, oaccount, dep_method, amount.bigDecimal, rate.bigDecimal, life_span
          , scrap_value.bigDecimal, frequency, currency)

    val mfEncoder: Encoder[Asset] = mfCodec.values.contramap(Asset.encodeIt)

    def base =
      sql""" SELECT id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount,
              dep_method, amount, rate, life_span, scrap_value, frequency, currency
             FROM   asset """

    def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Asset] =
      sql"""SELECT id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount,
              dep_method, amount, rate, life_span, scrap_value, frequency, currency
             FROM   asset
             WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val BY_ID: Query[String *: Int *: String *: EmptyTuple, Asset] =
      sql"""SELECT id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount,
              dep_method, amount, rate, life_span, scrap_value, frequency, currency
             FROM   asset
             WHERE id = $varchar AND modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val ALL: Query[Int *: String *: EmptyTuple, Asset] =
      sql"""SELECT id, name, description, changedate, enterdate, postingdate, company, modelid, account, oaccount,
              dep_method, amount, rate, life_span, scrap_value, frequency, currency
             FROM   asset
             WHERE  modelid = $int4 AND company = $varchar
             """.query(mfDecoder)

    val insert: Command[Asset] = sql"""INSERT INTO asset VALUES $mfEncoder """.command

    def insertAll(n: Int): Command[List[Asset.TYPE]] = sql"INSERT INTO asset VALUES ${mfCodec.values.list(n)}".command

    val upsert: Command[Asset] =
      sql"""
            INSERT INTO asset
             VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
             id                   = EXCLUDED.id,
             name                 = EXCLUDED.name,
             description          = EXCLUDED.description,
              account            = EXCLUDED.account
            """.command

    def DELETE: Command[(String, Int, String)] =
      sql"DELETE FROM asset WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
