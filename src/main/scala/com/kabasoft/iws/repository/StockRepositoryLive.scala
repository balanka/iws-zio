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

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}

final case class StockRepositoryLive(postgres: Resource[Task, Session[Task]]) extends StockRepository, MasterfileCRUD:

  import StockRepositorySQL._

  override def create(c: Stock): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)

  override def create(list: List[Stock]):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Stock.encodeIt), insertAll(list.size), list.size)
 
  override def modify(model: Stock):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, Stock.encodeIt3, UPDATE, 1)

  override def modify(models: List[Stock]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Stock.encodeIt3)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Stock]] = queryWithTx(postgres, p, ALL)

  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError,  Stock] = queryWithTxUnique(postgres, p, BY_ID)

  override def getByStoreArticle(p: (String, String, Int, String)): ZIO[Any, RepositoryError, List[Stock]] = queryWithTx(postgres, p, BY_4_STORE_ARTICLE)
  override def getByStore(p: (String, Int, String)): ZIO[Any, RepositoryError, List[Stock]] = queryWithTx(postgres, p, BY_STORE)
  override def getByArticle(p: (String, Int, String)): ZIO[Any, RepositoryError, List[Stock]] = queryWithTx(postgres, p, BY_ARTICLE)
  
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Stock]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  override def delete(p: (String, Int, String)):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, p, DELETE, 1)

  override def deleteAll(): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          session.execute(DELETE_ALL)
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1))
    
object StockRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, StockRepository] =
    ZLayer.fromFunction(new StockRepositoryLive(_))

private[repository] object StockRepositorySQL:
  
  private val mfCodec = (varchar *: varchar *: varchar *: numeric(12,2) *: numeric *:varchar *: varchar *: int4)
  
  val mfDecoder: Decoder[Stock] = mfCodec.map:
    case (_, store, article, quantity, price, charge, company, _) =>
      Stock.make(store, article, quantity.bigDecimal, charge, company)

  val mfEncoder: Encoder[Stock] = mfCodec.values.contramap(Stock.encodeIt)

  def base =
    sql""" SELECT id, store, article, quantity, 0.0 as price, charge, company, modelid
           FROM   stock """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Stock] =
    sql"""SELECT id, store, article, quantity, 0.0 as price, charge, company, modelid
           FROM   stock
           WHERE id  IN ( ${varchar.list(nr)} ) AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, 0.0 as price, charge, company, modelid
           FROM   stock
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_4_STORE_ARTICLE: Query[String *: String *: Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, 0.0 as price, charge, company, modelid
           FROM   stock
           WHERE store = $varchar AND article = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_ARTICLE: Query[String *: Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, 0.0 as price, charge, company, modelid
           FROM   stock
           WHERE article = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)
    
  val BY_STORE: Query[String *: Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, 0.0 as price, charge, company, modelid
           FROM   stock
           WHERE store = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)    
 
  val ALL: Query[Int *: String *: EmptyTuple, Stock] =
    sql"""SELECT id, store, article, quantity, 0.00 as price, charge, company, modelid
           FROM   stock
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Stock] = sql"""INSERT INTO stock VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[(String, String, String, BigDecimal, BigDecimal, String, String, Int)]] =
    sql"INSERT INTO stock VALUES ${mfCodec.values.list(n)}".command

  val UPDATE: Command[Stock.TYPE3] =
    sql"""UPDATE stock SET quantity = $numeric, charge = $varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command

  val updateQuantity: Command[BigDecimal *: String *: EmptyTuple] =
    sql"""UPDATE periodic_account_balance
            UPDATE SET
            quantity  = $numeric(12,2)
            WHERE id =$varchar
          """.command
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM stock WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command

  def DELETE_ALL: Command[Void] =
    sql"""DELETE FROM stock WHERE company = '-1000'""".command  
