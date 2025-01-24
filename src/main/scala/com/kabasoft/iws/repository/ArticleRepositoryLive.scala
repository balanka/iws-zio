package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Article
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case class ArticleRepositoryLive(postgres: Resource[Task, Session[Task]]) extends ArticleRepository, MasterfileCRUD:

  import ArticleRepositorySQL.*

  override def create(c: Article):  ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)

  override def create(list: List[Article]): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Article.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Article): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Article.encodeIt2, UPDATE, 1)
  override def modify(models: List[Article]): ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Article.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Article]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Article] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Article]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

  override def deleteAll(p: (List[String], Int, String)): ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, p, DELETE_ALL(p._1.size), p._1.size)
      .mapBoth(e => e, _ => p._1.size)
    
object ArticleRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, ArticleRepository] =
    ZLayer.fromFunction(new ArticleRepositoryLive(_))

private[repository] object ArticleRepositorySQL:
  def toInstant(localDateTime: LocalDateTime): Instant = localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  val mfCodec = (varchar *: varchar *: varchar *: varchar *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: varchar *: bool *: varchar *: varchar *: varchar *: varchar *: varchar *: varchar *: int4 *: timestamp *: timestamp *: timestamp)
  
  val mfEncoder: Encoder[Article] = mfCodec.values.contramap(Article.encodeIt)

  val mfDecoder: Decoder[Article] = mfCodec.map:
    case (id, name, description, parent, sprice, pprice, avgPrice, currency, stocked, quantityUnit, packUnit, stockAccount, expenseAccount, vatCode, company, modelid, enterdate, changedate, postingdate) =>
      Article(id, name, description, parent, sprice.bigDecimal, pprice.bigDecimal, avgPrice.bigDecimal, currency, stocked, quantityUnit, packUnit, stockAccount, expenseAccount, vatCode, company, modelid, toInstant(enterdate), toInstant(changedate), toInstant(postingdate))
  def base =
  sql""" SELECT id, name, description, parent, sprice, pprice, avg_price, currency, stocked, quantity_unit, pack_unit, stock_account, expense_account, vat_code, company, modelid, enterdate, changedate, postingdate
           FROM   article """

  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Article] =
  sql"""SELECT id, name, description, parent, sprice, pprice, avg_price, currency, stocked, quantity_unit, pack_unit, stock_account, expense_account, vat_code, company, modelid, enterdate, changedate, postingdate
           FROM   article
           WHERE id  IN ( ${varchar.list(nr)} ) AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Article] =
  sql"""SELECT id, name, description, parent, sprice, pprice, avg_price, currency, stocked, quantity_unit, pack_unit, stock_account, expense_account, vat_code, company, modelid, enterdate, changedate, postingdate
           FROM   article
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Article] =
  sql"""SELECT id, name, description, parent, sprice, pprice, avg_price, currency, stocked, quantity_unit, pack_unit, stock_account, expense_account, vat_code, company, modelid, enterdate, changedate, postingdate
           FROM   article
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Article] = sql"""INSERT INTO article (id, name, description, parent, sprice, pprice, avg_price
        , currency, stocked, quantity_unit, pack_unit, stock_account, expense_account, vat_code, company, modelid
        , enterdate, changedate, postingdate) VALUES $mfEncoder""".stripMargin.command
  
  def insertAll(n: Int): Command[List[Article.Article_Type3]] =
    sql"""INSERT INTO article (id, name, description, parent, sprice, pprice, avg_price, currency, stocked
         , quantity_unit, pack_unit, stock_account, expense_account, vat_code, company, modelid, enterdate, changedate
         , postingdate) VALUES ${mfCodec.values.list(n)}""".command
    
  val UPDATE: Command[Article.TYPE22] =
    sql"""UPDATE article
          SET name = $varchar, description = $varchar, parent = $varchar, sprice= $numeric, pprice= $numeric
          , avg_price= $numeric, currency =$varchar, stocked=$bool
           , quantity_unit=$varchar, pack_unit=$varchar, stock_account=$varchar, expense_account=$varchar, vat_code=$varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
  
  val updatePrices: Command[BigDecimal *: BigDecimal *: BigDecimal *: String *: Int *: String *: EmptyTuple] =
    sql"""UPDATE article
            UPDATE SET
            sprice                 = $numeric,
            pprice                 = $numeric,
            avg_price              = $numeric,
            WHERE id =$varchar AND modelid = $int4 AND  company =$varchar
          """.command
  
  def DELETE: Command[(String, Int, String)] =
       sql"DELETE FROM article WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command

  def DELETE_ALL(nr: Int): Command[(List[String], Int, String)] =
    sql"DELETE FROM article WHERE id  IN ( ${varchar.list(nr)} )  AND modelid = $int4 AND company = $varchar".command