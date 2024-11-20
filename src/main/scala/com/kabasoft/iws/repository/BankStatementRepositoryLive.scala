package com.kabasoft.iws.repository
import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankStatement, FinancialsTransaction}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import MasterfileCRUD.{UpdateCommand, InsertBatch}

import java.time.{Instant, LocalDateTime, ZoneId}


final case  class BankStatementRepositoryLive(postgres: Resource[Task, Session[Task]]
                                              , ftrRepo: FinancialsTransactionRepository)
  extends BankStatementRepository, MasterfileCRUD:

  import BankStatementRepositorySQL.*

  override def create(c: BankStatement, flag: Boolean):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[BankStatement]):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, 
    list.map(BankStatement.encodeIt), insertAll(list.size), list.size)
  override def modify(model: BankStatement):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, BankStatement.encodeIt2, UPDATE, 1)
  override def modify(models: List[BankStatement]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, BankStatement.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[BankStatement]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Long, Int, String)): ZIO[Any, RepositoryError, BankStatement] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[BankStatement]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  def delete(p: (Long, Int, String)):ZIO[Any, RepositoryError, Int]=  executeWithTx(postgres, p, DELETE, 1)
//    val newLines = models.flatMap(ftr=>ftr.lines.filter(_.id == -1L).map(l => l.copy(transid = ftr.id)))
//    val deletedLine = models.flatMap(ftr=>ftr.lines.filter(_.transid == -2L))
//    val deletedLineIds = deletedLine.map(line => line.id)
//    val oldLines2Update = models.flatMap(ftr=>ftr.lines.filter(_.id > 0L).map(l => l.copy(transid = ftr.id)))
//    val insertDetailsCmd = FinancialsTransactionRepositorySQL.insertAllDetails(newLines.length)
//    val updateDetailsCmd = FinancialsTransactionRepositorySQL.UPDATE_DETAILS
//    val createDetailsCmd = InsertBatch(newLines, FinancialsTransactionDetails.encodeIt, insertDetailsCmd)
//    val updateDetailsCmds = IwsCommandLP2(oldLines2Update, FinancialsTransactionDetails.encodeIt2, updateDetailsCmd)
//    val deleteDetailsCmd = IwsCommandLP2(oldLines2Update, FinancialsTransactionDetails.encodeIt3, DELETE_DETAILS)
//    val updateFtrCmd = models.map(ftr=>IwsCommand(ftr, FinancialsTransaction.encodeIt2, FinancialsTransactionRepositorySQL.UPDATE))
//    executeBatchWithTx2(postgres, updateFtrCmd, List(deleteDetailsCmd), List(createDetailsCmd), List(updateDetailsCmds))
// for {
//    idx<-queryWithTxUnique(postgres, TRANS_ID)
//    ftr = c.copy(id = idx, lines = c.lines.map(l=>l.copy(transid = idx)))
//    details <- createDetails(ftr.lines)
//    result <- executeWithTx(postgres, ftr, insert, 1)
//  } yield result +details
  override def post(bs: List[BankStatement], transactions: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = {
    val bsCmds = bs.map(e => UpdateCommand(e, BankStatement.encode, POST_BANK_STATEMENT))
    val cmdx = FinancialsTransactionRepositorySQL.insertAll(transactions.length)
    val ftsCmds = InsertBatch(transactions, FinancialsTransaction.encodeIt, cmdx)
    executeBatchWithTx(postgres, bsCmds, List(ftsCmds))
    ZIO.succeed(transactions.size + 1)
    // for {
    //    idx<-queryWithTxUnique(postgres, TRANS_ID)
    //    ftr = c.copy(id = idx, lines = c.lines.map(l=>l.copy(transid = idx)))
    //    details <- createDetails(ftr.lines)
    //    result <- executeWithTx(postgres, ftr, insert, 1)
    //  } yield result +details
  }

object BankStatementRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & FinancialsTransactionRepository, Throwable, BankStatementRepository] =
    ZLayer.fromFunction(new BankStatementRepositoryLive(_, _))

private[repository] object BankStatementRepositorySQL:

  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val bankStatementCodec =
    (int8 *: varchar *: timestamp *: timestamp *: varchar *: varchar *: varchar *: varchar *: varchar *: numeric(12,2) *: varchar *: varchar *: varchar *: varchar *: bool *: int4 *:  int4 )

  val mfDecoder: Decoder[BankStatement] = bankStatementCodec.map:
    case (id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bankCode, amount, currency, info, company, companyIban, posted, modelid, period) =>
      BankStatement(id, depositor, toInstant(postingdate), toInstant(valuedate), postingtext, purpose, beneficiary, accountno, bankCode, amount.bigDecimal, currency, info, company, companyIban, posted, modelid, period) 
  
  val mfEncoder: Encoder[BankStatement] = bankStatementCodec.values.contramap(BankStatement.encodeIt)
  
  def base =
    sql""" SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement """

  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), BankStatement] =
    sql"""
           SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement
           WHERE id  IN ${int8.list(nr)} AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, BankStatement] =
    sql"""
           SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, BankStatement] =
    sql"""
           SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[BankStatement] = sql"""INSERT INTO bankstatement VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[(Long, String,  LocalDateTime, LocalDateTime, String,  String, String, String, String, BigDecimal, String, String, String, String, Boolean, Int, Int)]] =
    sql"INSERT INTO bankstatement VALUES ${bankStatementCodec.values.list(n)}".command

  val UPDATE: Command[BankStatement.TYPE3] =
    sql"""UPDATE bankstatement SET valuedate=$timestamp, accountno= $varchar, bank_code= $varchar
          WHERE id=$int8 and modelid=$int4 and company= $varchar""".command
    
  val upsert: Command[BankStatement] =
    sql"""INSERT INTO bankstatement
           VALUES $mfEncoder ON CONFLICT(id, company) DO UPDATE SET
           valuedate              = EXCLUDED.valuedate,
           accountno              = EXCLUDED.accountno,
            bank_code             = EXCLUDED.bank_code,
            info                  = EXCLUDED.info,
            amount                = EXCLUDED.changedate,
            postingdate           = EXCLUDED.postingdate,
            period                = EXCLUDED.period,
            postingtext           = EXCLUDED.postingtext,
            purpose               = EXCLUDED.purpose,
            currency              = EXCLUDED.currency,
            company               = EXCLUDED.company,
            company_iban          = EXCLUDED.company_iban
          """.command.to[BankStatement]

  val POST_BANK_STATEMENT: Command[Boolean *: Int *: Long *: Int *: String *: EmptyTuple] =
    sql"""UPDATE bankstatement SET
           posted                 = $bool,
           period                 = $int4,
          WHERE  id = $int8 AND modelid = $int4 AND company = $varchar
          """.command

  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM bankstatement WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command