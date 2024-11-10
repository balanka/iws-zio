package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.Article
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}

import java.time.{Instant, LocalDateTime, ZoneId}

final case class ArticleRepositoryLive(postgres: Resource[Task, Session[Task]]) extends ArticleRepository, MasterfileCRUD:

  import ArticleRepositorySQL.*

  override def create(c: Article, flag: Boolean):  ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, if (flag) upsert else insert, 1)

  override def create(list: List[Article]): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, list.map(Article.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Article): ZIO[Any, RepositoryError, Int] = create(model, true)
  override def modify(models: List[Article]): ZIO[Any, RepositoryError, Int] = models.map(modify).flip.map(_.sum)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Article]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Article] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Article]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

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
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
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

  val insert: Command[Article] = sql"INSERT INTO masterfile VALUES $mfEncoder".command
  def insertAll(n: Int): Command[List[Article.Article_Type3]] = sql"INSERT INTO article VALUES ${mfCodec.values.list(n)}".command

  val upsert: Command[Article] =
  sql"""INSERT INTO article
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           id                     = EXCLUDED.id,
           name                   = EXCLUDED.name,
           description            = EXCLUDED.description,
            account               = EXCLUDED.parent
          """.command
  
  val updatePrices: Command[BigDecimal *: BigDecimal *: BigDecimal *: String *: Int *: String *: EmptyTuple] =
    sql"""UPDATE article
            UPDATE SET
            sprice                 = $numeric(12,2),
            pprice               = $numeric(12,2),
            avgPrice                 = $numeric(12,2),
            WHERE id =$varchar AND modelid = $int4 AND  company =$varchar
          """.command
    
  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(String, Int, String)] =
  sql"DELETE FROM article WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
