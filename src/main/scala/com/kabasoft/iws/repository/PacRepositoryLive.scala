package com.kabasoft.iws.repository

import cats.effect.Resource
import cats.syntax.all._
import cats._
import skunk._
import skunk.codec.all._
import skunk.implicits._
import zio.interop.catz._
import zio.{Task, ZIO, ZLayer }
import com.kabasoft.iws.domain.PeriodicAccountBalance
import com.kabasoft.iws.domain.AppError.RepositoryError
import java.time.{ Instant, LocalDateTime, ZoneId }

final case class PacRepositoryLive(postgres: Resource[Task, Session[Task]]) extends PacRepository, MasterfileCRUD:

  import PacRepositorySQL._

  override def create(c: PeriodicAccountBalance):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)

  override def create(list: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int] =
    executeWithTx(postgres, list.distinct.map(PeriodicAccountBalance.encodeIt), insertAll(list.size), list.distinct.size)
  
  override def update(models: List[PeriodicAccountBalance]):ZIO[Any, RepositoryError, Int] =
    executeBatchWithTxK(postgres, models , UPDATE, PeriodicAccountBalance.encodeIt2)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] = queryWithTx(postgres, p, ALL)

  override def getById(p: (String, Int, String)): ZIO[Any, RepositoryError, PeriodicAccountBalance] = queryWithTxUnique(postgres, p, BY_ID)

  override def getBy(ids: List[String], modelid: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    if (ids.isEmpty) ZIO.succeed(List.empty)
    else queryWithTx(postgres, (ids, 106, company), ALL_BY_ID(ids.length))

  override def getByParent(ids: List[String], from:Int, to:Int, modelid: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    if (ids.isEmpty) ZIO.succeed(List.empty)
    else queryWithTx(postgres, (ids, from, to, PeriodicAccountBalance.MODELID, company), BY_ACCOUNT_PERIOD_FROM_TO(ids.length))
  
  override def findBalance4Period( period: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]] =
    queryWithTx(postgres, (period, company), FIND_4_PERIOD_QUERY)
    
  override def find4AccountPeriod(account: String, fromPeriod: Int, toPeriod: Int, company: String):ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]=
    queryWithTx(postgres, (account, fromPeriod, toPeriod, company), BALANCE_4_ACCOUNT_PERIOD)
  
  override def findBalance4Period(fromPeriod: Int, toPeriod: Int, company: String): ZIO[Any, RepositoryError, List[PeriodicAccountBalance]]=
    queryWithTx(postgres, (fromPeriod, toPeriod, company), BALANCE_QUERY)
  
  override def deleteAll(): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
           session.execute(DELETE_TEST)
       .mapBoth(e => RepositoryError(e.getMessage), _ => 1)
    
object PacRepositoryLive:
  
  val live: ZLayer[Resource[Task, Session[Task]], RepositoryError, PacRepository] =
    ZLayer.fromFunction(new PacRepositoryLive(_))

object PacRepositorySQL:
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant
  val mfCodec =
    (varchar *: varchar *: int4 *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *: numeric(12, 2) *:numeric(12, 2) *: numeric(12, 2) *: varchar *: varchar *: varchar *: int4)
    
  val mfCodec2 =
    (text *: varchar *: int4 *: numeric *: numeric *: numeric *: numeric *: numeric *: numeric *:varchar *: varchar *: varchar *: int4)
  val mfDecoder: Decoder[PeriodicAccountBalance] = mfCodec.map:
    case (id, account, period, idebit, icredit, debit, credit, bdebit, bcredit, currency, company, name, modelid) =>
      PeriodicAccountBalance(id, account, period, idebit.bigDecimal, icredit.bigDecimal, debit.bigDecimal
        , credit.bigDecimal, bdebit.bigDecimal, bcredit.bigDecimal, currency, company, name, modelid)
      
  val mfDecoder2: Decoder[PeriodicAccountBalance] = mfCodec2.map :
    case (id, account, period, idebit, icredit, debit, credit, bdebit, bcredit, currency, company, name, modelid) =>
      PeriodicAccountBalance(id, account, period, idebit.bigDecimal, icredit.bigDecimal, debit.bigDecimal
        , credit.bigDecimal, bdebit.bigDecimal, bcredit.bigDecimal, currency, company, name, modelid)
  
  val mfEncoder: Encoder[PeriodicAccountBalance] = mfCodec.values.contramap(PeriodicAccountBalance.encodeIt)

  def base =
    sql""" SELECT id, account, period, idebit, icredit, debit, credit, b_debit, b_credit,  currency, company, name, modelid
           FROM   periodic_account_balance """
  
  def ALL_BY_ID(nr: Int): Query[(List[String], Int, String), PeriodicAccountBalance] =
    sql"""SELECT id, account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid
           FROM   periodic_account_balance
           WHERE id  IN (${varchar.list(nr)} ) AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[String *: Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""SELECT id, account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid
           FROM   periodic_account_balance
           WHERE id = $varchar AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""SELECT id, account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid
           FROM   periodic_account_balance
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[PeriodicAccountBalance] =
    sql"""INSERT INTO periodic_account_balance 
         (id , account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid) VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[PeriodicAccountBalance.TYPE]] =
    sql"""INSERT INTO periodic_account_balance 
         (id , account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid)
          VALUES ${mfCodec.values.list(n)}""".command
  
  val UPDATE: Command[BigDecimal *: BigDecimal *: BigDecimal *: BigDecimal *: BigDecimal *: BigDecimal *:String *: Int *: String *:EmptyTuple] =
    sql"""UPDATE periodic_account_balance
            UPDATE SET
            idebit                = $numeric,
            icredit               = $numeric,
            debit                 = $numeric,
            credit                = $numeric,
            b_debit               = $numeric,
            b_credit               = $numeric
            WHERE id =$varchar AND period = $int4 AND  company =$varchar
          """.command  

  val FIND_QUERY: Query[Int *: Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""select id , account, period, idebit, icredit, debit, credit, b_debit, b_credit,currency, company, name, modelid
       FROM  peridic_account_balance
       WHERE period BETWEEN $int4 AND  $int4 and  company =$varchar
       order By account desc
       """.query(mfDecoder)

  val FIND_4_PERIOD_QUERY: Query[Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""select id , account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid
       FROM  periodic_account_balance
       WHERE period = $int4 AND  company =$varchar
       order By account desc
       """.query(mfDecoder)
//  id: String,account: String,period: Int,idebit: BigDecimal,icredit: BigDecimal,debit: BigDecimal,
//  credit: BigDecimal,currency: String,company: String,name: String,
  val BALANCE_QUERY: Query[Int *: Int *:String *: EmptyTuple, PeriodicAccountBalance] =
   sql"""select Max(id) as id, account, Max(period) as period,SUM(idebit) as idebit,
      SUM(icredit) as icredit, SUM(debit) as debit, SUM(credit) as credit, SUM(b_debit) as b_debit, SUM(b_credit) as b_credit,
      currency, company, name, modelid
      FROM  periodic_account_balance
      WHERE period BETWEEN $int4 AND  $int4 and  company =$varchar
      group By(account, currency, company, name, modelid)
      order By account desc
      """.query(mfDecoder2)

  val BALANCE_4_ACCOUNT_PERIOD: Query[String *: Int *: Int *: String *: EmptyTuple, PeriodicAccountBalance] =
    sql"""select id, account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid
       FROM  periodic_account_balance
       WHERE  account = $varchar AND
              period BETWEEN $int4 AND  $int4 AND
              company = $varchar
       Order By id desc
       """.query(mfDecoder)
  
  def BY_ACCOUNT_PERIOD_FROM_TO(nr: Int): Query[(List[String], Int, Int, Int,String), PeriodicAccountBalance] =
    sql"""select Max(id) as id, account, Max(period) as period,SUM(idebit) as idebit,
      SUM(icredit) as icredit, SUM(debit) as debit, SUM(credit) as credit, SUM(b_debit) as b_debit, SUM(b_credit) as b_credit,
      currency, company, name, modelid
      FROM  periodic_account_balance
      WHERE account  IN (${varchar.list(nr)} )  AND period BETWEEN $int4 AND  $int4  AND modelid = $int4  AND company =$varchar
      group By(account, currency, company, name, modelid)
      order By account desc
      """.query(mfDecoder2)
    
    
//    sql"""SELECT id, account, period, idebit, icredit, debit, credit, b_debit, b_credit, currency, company, name, modelid
//           FROM   periodic_account_balance
//           WHERE account  IN (${varchar.list(nr)} ) AND  period BETWEEN  $int4 AND $int4  AND modelid = $int4 AND company = $varchar
//           """.query(mfDecoder)
           

  def DELETE: Command[(String, String)] =
    sql"""DELETE FROM periodic_account_balance WHERE id = $varchar  AND company = $varchar""".command
    
  def DELETE_TEST: Command[Void] =
    sql"""DELETE FROM periodic_account_balance WHERE  company ='-1000'""".command
  def DELETE_ALL(nr: Int): Command[(List[String], String)] =
    sql"""DELETE FROM periodic_account_balance WHERE id IN ( ${varchar.list(nr)} ) AND company = $varchar""".command