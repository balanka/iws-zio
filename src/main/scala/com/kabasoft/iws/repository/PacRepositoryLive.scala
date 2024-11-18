package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all.*
import cats._
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{ Instant, LocalDateTime, ZoneId }

final case class PacRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PacRepository, MasterfileCRUD:

  import PacRepositorySQL._

  override def create(c: PeriodicAccountBalance, flag: Boolean):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)

  override def create(list: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.map(PeriodicAccountBalance.encodeIt), insertAll(list.size), list.size)
    
//  override def modify(model: PeriodicAccountBalance):ZIO[Any, RepositoryError, Int]= 
//    executeWithTx(postgres, model, PeriodicAccountBalance.encodeIt2, UPDATE, 1)
//
//  override def modify(models: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int] = 
//    executeBatchWithTxK(postgres, models, UPDATE, PeriodicAccountBalance.encodeIt2)

  override def update(models: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int] =
    executeBatchWithTxK(postgres, models , UPDATE, PeriodicAccountBalance.encodeIt2)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] = queryWithTx(postgres, p, ALL)

  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, PeriodicAccountBalance] = queryWithTxUnique(postgres, p, BY_ID)

  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  
  override def findBalance4Period( period: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    queryWithTx(postgres, (period, company), FIND_4_PERIOD_QUERY)
    
  override def find4AccountPeriod(account: String, toPeriod: Int, company: String):ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]={
    val year = toPeriod.toString.slice(0, 4)
    val fromPeriod = year.concat("01").toInt
    queryWithTx(postgres, (account, fromPeriod, toPeriod, company), BALANCE_4_ACCOUNT_PERIOD)
  }
    
  def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]=
    queryWithTx(postgres, (fromPeriod, toPeriod, company), BALANCE_QUERY)
    
object PacRepositoryLive:
  
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PacRepository] =
    ZLayer.fromFunction(new PacRepositoryLive(_))

object PacRepositorySQL:
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
  val mfCodec =
    (varchar *: varchar *: int4 *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: varchar *: varchar *: varchar *: int4)
    
  val mfCodec2 =
    (text *: varchar *: int4 *: numeric *: numeric *: numeric *: numeric *: varchar *: varchar *: varchar *: int4)
  val mfDecoder: Decoder[PeriodicAccountBalance] = mfCodec.map:
    case (id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid) =>
      PeriodicAccountBalance(id, account, period, idebit.bigDecimal, icredit.bigDecimal, debit.bigDecimal
        , credit.bigDecimal, currency, company, name, modelid)
      
  val mfDecoder2: Decoder[PeriodicAccountBalance] = mfCodec2.map :
    case (id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid) =>
      PeriodicAccountBalance(id, account, period, idebit.bigDecimal, icredit.bigDecimal, debit.bigDecimal
        , credit.bigDecimal, currency, company, name, modelid)
  
  val mfEncoder: Encoder[PeriodicAccountBalance] = mfCodec.values.contramap(PeriodicAccountBalance.encodeIt)

  def base =
    sql""" SELECT id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid
           FROM   periodic_account_balance """
  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), PeriodicAccountBalance] =
    sql"""SELECT id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid
           FROM   periodic_account_balance
           WHERE id  IN ${varchar.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""SELECT id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid
           FROM   periodic_account_balance
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""SELECT id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid
           FROM   periodic_account_balance
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[PeriodicAccountBalance] = sql"""INSERT INTO periodic_account_balance VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[PeriodicAccountBalance.TYPE]] =
    sql"INSERT INTO periodic_account_balance VALUES ${mfCodec.values.list(n)}".command

  val upsert: Command[PeriodicAccountBalance] =
    sql"""INSERT INTO periodic_account_balance
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           account                = EXCLUDED.account,
           name                   = EXCLUDED.name,
           idebit                 = EXCLUDED.idebit,
            icredit               = EXCLUDED.icredit,
            debit                 = EXCLUDED.debit,
            credit                = EXCLUDED.credit,
          """.command


  
  val UPDATE: Command[BigDecimal *: BigDecimal *: BigDecimal *: BigDecimal *: String *: Int *: String *:EmptyTuple] =
    sql"""UPDATE periodic_account_balance
            UPDATE SET
            idebit                 = $numeric(12,2),
            icredit               = $numeric(12,2),
            debit                 = $numeric(12,2),
            credit                = $numeric(12,2)
            WHERE id =$varchar AND period = $int4 AND  company =$varchar
          """.command  

  val FIND_QUERY: Query[Int *: Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""select id , account, period, idebit, icredit, debit, credit, currency, company, name, modelid
       FROM  peridic_account_balance
       WHERE period BETWEEN $int4 AND  $int4 and  company =$varchar
       order By account desc
       """.query(mfDecoder)

  val FIND_4_PERIOD_QUERY: Query[Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""select id , account, period, idebit, icredit, debit, credit, currency, company, name, modelid
       FROM  periodic_account_balance
       WHERE period = $int4 AND  company =$varchar
       order By account desc
       """.query(mfDecoder)

  val BALANCE_QUERY: Query[Int *: Int *:String *: EmptyTuple, PeriodicAccountBalance] =
   sql"""select Max(id) as id, account, Max(period) as period,SUM(idebit) as idebit,
      SUM(icredit) as icredit, SUM(debit) as debit, SUM(credit) as credit,
      currency, company, name, modelid
      FROM  periodic_account_balance
      WHERE period BETWEEN $int4 AND  $int4 and  company =$varchar
      group By(account, currency, company, name, modelid)
      order By account desc
      """.query(mfDecoder)

  val BALANCE_4_ACCOUNT_PERIOD: Query[String *: Int *: Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""select id, account, period, idebit, icredit, debit, credit, currency, company, name, modelid
       FROM  periodic_account_balance
       WHERE  account = $varchar AND
              period BETWEEN $int4 AND  $int4 AND
              company = $varchar
       Order By id desc
       """.query(mfDecoder)

  val BALANCE_4_ACCOUNT_PERIOD_GROUPED: Query[String *:Int *: Int *: String *: EmptyTuple, PeriodicAccountBalance] =
  sql"""select Max(id) as id, account, Max(period) as period,SUM(idebit) as idebit,
       SUM(icredit) as icredit, SUM(debit) as debit, SUM(credit) as credit,
       currency, company, name, modelid
       FROM  periodic_account_balance
       WHERE  account = $varchar AND
              period BETWEEN $int4 AND  $int4 AND
              company = $varchar
       Group By(account, currency, company, name, modelid)
       Order By account desc
       """.query(mfDecoder2)
  
  
  def DELETE: Command[(String, Int, String)] =
    sql"DELETE FROM periodic_account_balance WHERE id = $varchar AND modelid = $int4 AND company = $varchar".command
