package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.interop.catz.*
import zio.prelude.FlipOps
import zio.stream.ZStream
import zio.stream.interop.fs2z.*
import zio.{Chunk, Task, UIO, ZIO, ZLayer}
import java.time.LocalDate
import com.kabasoft.iws.domain.Stock
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{ Instant, LocalDateTime, ZoneId, ZoneOffset }

final case class StockRepositoryLive(postgres: Resource[Task, Session[Task]]) extends StockRepository, MasterfileCRUD:

  import StockRepositorySQL._

  override def create(c: Stock, flag: Boolean): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, if (flag) upsert else insert, 1)

  override def create(list: List[Stock]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Stock.encodeIt), insertAll(list.size), list.size)
 
  override def modify(model: Stock):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, Stock.encodeIt3, UPDATE, 1)

  override def modify(models: List[Stock]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Stock.encodeIt3)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Stock]] = queryWithTx(postgres, p, ALL)

  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError,  Stock] = queryWithTxUnique(postgres, p, BY_ID)

  override def getByStoreArticle(p: (String, String, Int, String)): ZIO[Any, RepositoryError, List[Stock]] = queryWithTx(postgres, p, BY_4_STORE_ARTICLE)
  
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Stock]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, p, DELETE, 1)

object StockRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, StockRepository] =
    ZLayer.fromFunction(new StockRepositoryLive(_))

private[repository] object StockRepositorySQL:
  
  private val mfCodec = (varchar *: varchar *: varchar *: numeric(12,2) *:varchar *: varchar *: int4)
  
  val mfDecoder: Decoder[Stock] = mfCodec.map:
    case (_, store, article, quantity, charge, company, _) =>
      Stock.make(store, article, quantity.bigDecimal, charge, company)

  val mfEncoder: Encoder[Stock] = mfCodec.values.contramap(Stock.encodeIt)

  def base =
    sql""" SELECT id, store, article, quantity, charge, company, modelid
           FROM   stock """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Stock] =
    sql"""SELECT id, store, article, quantity, charge, company, modelid
           FROM   stock
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, charge, company, modelid
           FROM   stock
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_4_STORE_ARTICLE: Query[String *: String *: Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, charge, company, modelid
           FROM   stock
           WHERE store = $varchar AND article = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, charge, company, modelid
           FROM   stock
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Stock] = sql"""INSERT INTO stock VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[(String, String, String, BigDecimal, String, String, Int)]] =
    sql"INSERT INTO stock VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Stock.TYPE3] =
    sql"""UPDATE stock SET quantity = $numeric, charge = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
    
  val upsert: Command[Stock] =
    sql"""INSERT INTO stock
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
            id                     = EXCLUDED.id,
            store                  = EXCLUDED.store,
            article                = EXCLUDED.article,
            quantity               = EXCLUDED.quantity,
            charge                 = EXCLUDED.charge,
            company                = EXCLUDED.company,
            modelid               = EXCLUDED.modelid,
          """.command
    
  val updateQuantity: Command[BigDecimal *: String *: EmptyTuple] =
    sql"""UPDATE periodic_account_balance
            UPDATE SET
            quantity  = $numeric(12,2)
            WHERE id =$varchar
          """.command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM stock WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
