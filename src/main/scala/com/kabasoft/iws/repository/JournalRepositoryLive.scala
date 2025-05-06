package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.Journal
import com.kabasoft.iws.domain.AppError.RepositoryError


import java.time.{ Instant, LocalDateTime, ZoneId }

final case class JournalRepositoryLive(postgres: Resource[Task, Session[Task]]) extends JournalRepository, MasterfileCRUD:

  import JournalRepositorySQL._
  
  override def create(c: Journal):ZIO[Any, RepositoryError, Int]=executeWithTx(postgres, c, insert, 1)
  override def create(list: List[Journal]):ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(Journal.encodeIt), insertAll(list.size), list.size)
  override def all(p: (Int, String)):ZIO[Any, RepositoryError, List[Journal]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Long,  String)):ZIO[Any, RepositoryError, Journal] = queryWithTxUnique(postgres, p, BY_ID)
  override def getByPeriod(period: Int,  company: String): ZIO[Any, RepositoryError, List[Journal]] = 
    queryWithTx(postgres, (period, company), ALL_BY_PERIOD)
  
  override def find4Period( account:String, fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]] =
    queryWithTx(postgres, (account, fromPeriod, toPeriod, company), FIND_4_PERIOD_QUERY)
  
  private def find4AccountPeriod(account: String, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[Journal]]=
    val fromPeriod = toPeriod.toString.slice(0, 4).concat("01").toInt
    queryWithTx(postgres, (account, fromPeriod, toPeriod, company), BALANCE_4_ACCOUNT_PERIOD)
  
  private def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String):ZIO[Any, RepositoryError, List[Journal]]=
    queryWithTx(postgres, (fromPeriod, toPeriod, company), BALANCE_QUERY)

  override def deleteAllTest(): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
        session.execute(DELETE_TEST)
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1)
    
object JournalRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, JournalRepository] =
    ZLayer.fromFunction(new JournalRepositoryLive(_))

object JournalRepositorySQL:
  def toInstant(localDateTime: LocalDateTime): Instant = localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
  val mfCodec =
    (int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *:int4 *: numeric(12,2) *: numeric(12,2) *: numeric(12,2) *: numeric(12,2) *: numeric(12,2) *: varchar *: bool *: varchar *: int4 *: int4 *: varchar *: int4)
  val mfCodec2 =
    (int8 *: int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: varchar *: bool *: varchar *: int4 *: int4 *: varchar *: int4)

  val mfDecoder: Decoder[Journal] = mfCodec2.map :
    case (id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
         , icredit, credit, currency, side, text, month, year, company, modelid) =>
      Journal (id, transid, oid, account, oaccount, toInstant(transdate), toInstant(postingdate), toInstant(enterdate)
        , period, amount.bigDecimal, idebit.bigDecimal, debit.bigDecimal, icredit.bigDecimal, credit.bigDecimal
        ,currency, side, text, month, year, company, modelid)


  val mfEncoder: Encoder[Journal] = mfCodec.values.contramap(Journal.encodeIt)

  def base =
    sql""" id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
         , icredit, credit,currency, side, text, month, year, company, modelid
           FROM   journal """
    
  def ALL_BY_PERIOD: Query[Int *: String *: EmptyTuple, Journal] =
    sql"""SELECT id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
         , icredit, credit,currency, side, text, month, year, company, modelid
           FROM   journal
           WHERE period = $int4  AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: String *: EmptyTuple, Journal] =
    sql"""SELECT id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
         , icredit, credit,currency, side, text, month, year, company, modelid
           FROM   journal
           WHERE id = $int8  AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, Journal] =
    sql"""SELECT id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
         , icredit, credit,currency, side, text, month, year, company, modelid
           FROM   journal
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[Journal] =
    sql"""INSERT INTO journal (
          transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
          , icredit, credit,currency, side, text, month, year, company, modelid)
          VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[Journal.TYPE]] =
    sql"""INSERT INTO journal (
         transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
         , icredit, credit,currency, side, text, month, year, company, modelid )
          VALUES ${mfCodec.values.list(n)}""".command
  

  val FIND_QUERY: Query[Int *: Int *: String *: EmptyTuple, Journal] =
    sql"""select id , account, period, idebit, icredit, debit, credit, currency, company, modelid
       FROM  journal
       WHERE period between $int4 AND  $int4 and  company =$varchar
        order By account desc
       """.query(mfDecoder)

  val FIND_4_PERIOD_QUERY: Query[String  *: Int *: Int *: String *: EmptyTuple, Journal] =
    sql"""SELECT id, transid, oid, account, oaccount, transdate, enterdate, postingdate, period, amount, idebit, debit
         , icredit, credit,currency, side, text, month, year, company, modelid
       FROM journal
       WHERE account=$varchar AND period between  $int4 and  $int4 AND  company =$varchar
       order By period desc
       """.query(mfDecoder)

  val BALANCE_QUERY: Query[Int *: Int *:String *: EmptyTuple, Journal] =
    sql"""select Max(id) as id, account, Max(period) as period,SUM(idebit) as idebit,
      SUM(icredit) as icredit, SUM(debit) as debit, SUM(credit) as credit,
      currency, company, modelid
      FROM  journal
      WHERE period between $int4 AND  $int4 and  company =$varchar
      group By account, currency, company, modelid
      order By account desc
      """.query(mfDecoder)

  val BALANCE_4_ACCOUNT_PERIOD: Query[String *:Int *: Int *: String *: EmptyTuple, Journal] =
    sql"""select Max(id) as id, account, Max(period) as period,SUM(idebit) as idebit,
       SUM(icredit) as icredit, SUM(debit) as debit, SUM(credit) as credit,
       currency, company, modelid
       FROM  journal
       WHERE  account = $varchar AND
              period between ($int4 AND  $int4) AND
              company = $varchar
       group By account, currency, company, modelid)
       order By account desc
       """.query(mfDecoder)

  def DELETE_TEST: Command[Void] = sql"""DELETE FROM journal WHERE  company = '-1000'""".command
