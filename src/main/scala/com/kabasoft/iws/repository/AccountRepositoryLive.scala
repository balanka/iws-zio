package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.prelude.FlipOps
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.Account
import com.kabasoft.iws.domain.AppError.RepositoryError

import java.time.{Instant, LocalDateTime, ZoneId}

final case class AccountRepositoryLive(postgres: Resource[Task, Session[Task]]) extends AccountRepository, MasterfileCRUD:

  import AccountRepositorySQL._

  override def create(c: Account): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Account]): ZIO[Any, RepositoryError, Int] =  executeWithTx(postgres, list.map(Account.encodeIt), insertAll(list.size), list.size)
  override def modify(model: Account): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, model, Account.encodeIt2, UPDATE, 1)
  override def modify(models: List[Account]): ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, Account.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[Account]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, Account] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[Account]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  override def delete(p: (String, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(p: List[(String, Int, String)]): ZIO[Any, RepositoryError, Int] = p.map(l => executeWithTx(postgres, l, DELETE, 1)).flip.map(_.size)

object AccountRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, AccountRepository] =
    ZLayer.fromFunction(new AccountRepositoryLive(_))

private[repository] object AccountRepositorySQL:

  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val mfCodec =
    (varchar *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: varchar *: int4 *: varchar *: bool *: bool *: varchar *: numeric(12,2 ) *: numeric(12,2)*: numeric(12,2) *: numeric(12,2))

  val mfDecoder: Decoder[Account] = mfCodec.map :
    case (id, name, description, enterdate, changedate, postingdate, company, modelid, account, isDebit, balancesheet, currency, idebit, icredit, debit, credit) =>
      Account(id, name, description, toInstant(enterdate), toInstant(changedate), toInstant(postingdate), company, modelid, account, isDebit, balancesheet, currency, idebit.bigDecimal, icredit.bigDecimal, debit.bigDecimal, credit.bigDecimal)

  val mfEncoder: Encoder[Account] = mfCodec.values.contramap(Account.encodeIt)

  def base =
    sql""" SELECT id, name, description, enterdate, changedate, postingdate, company, modelid, account, is_debit, balancesheet, currency, idebit, icredit, debit, credit
           FROM   account ORDER BY id ASC"""
  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), Account] =
    sql"""SELECT id, name, description, enterdate, changedate, postingdate, company, modelid, account, is_debit, balancesheet, currency, idebit, icredit, debit, credit
           FROM   account
           WHERE id  IN (${varchar.list(nr)}) AND  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, Account] =
    sql""" SELECT id, name, description, enterdate, changedate, postingdate, company, modelid, account, is_debit, balancesheet, currency, idebit, icredit, debit, credit
           FROM   account
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Account] =
    sql"""SELECT id, name, description, enterdate, changedate, postingdate, company, modelid, account, is_debit, balancesheet, currency, idebit, icredit, debit, credit
           FROM   account
           WHERE  modelid = $int4 AND company = $varchar
           ORDER BY id ASC""".query(mfDecoder)

  val insert: Command[Account] =
    sql"""INSERT INTO account
          (id, name, description, enterdate, postingdate, changedate, company, modelid, account,is_debit, balancesheet
          , currency, idebit, icredit, debit, credit )
          VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[Account.TYPE]] =
    sql"""INSERT INTO account (id, name, description, enterdate, postingdate, changedate, company, modelid, account,is_debit, balancesheet
  , currency, idebit, icredit, debit, credit )
  VALUES ${mfCodec.values.list(n)}""".command

  val UPDATE: Command[Account.TYPE2] =
    sql"""UPDATE account
          SET name = $varchar, description = $varchar, account = $varchar, is_debit=$bool
          , balancesheet= $bool, currency =$varchar
          WHERE id=$varchar and modelid=$int4 and company= $varchar""".command
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM account WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
