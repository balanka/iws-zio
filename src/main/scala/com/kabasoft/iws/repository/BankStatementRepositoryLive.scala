package com.kabasoft.iws.repository
import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankStatement, FinancialsTransaction, FinancialsTransactionDetails}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.interop.catz.*
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import MasterfileCRUD.{ExecCommand, InsertBatch, UpdateCommand}

import java.time.{Instant, LocalDateTime, ZoneId}


final case  class BankStatementRepositoryLive(postgres: Resource[Task, Session[Task]]
                                              , ftrRepo: FinancialsTransactionRepository)
  extends BankStatementRepository, MasterfileCRUD:

  import BankStatementRepositorySQL.*

  override def create(c: BankStatement):ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, c, insert, 1)
  override def create(list: List[BankStatement]):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, 
    list.map(BankStatement.encodeIt), insertAll(list.size), list.size)
  override def modify(model: BankStatement):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, BankStatement.encodeIt2, UPDATE, 1)
  override def modify(models: List[BankStatement]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, BankStatement.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[BankStatement]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Long, Int, String)): ZIO[Any, RepositoryError, BankStatement] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[BankStatement]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  def delete(p: (Long, Int, String)):ZIO[Any, RepositoryError, Int]=  executeWithTx(postgres, p, DELETE, 1)
  
  override def post(bs: List[BankStatement], models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =  for {
    (createTransactionCmd, createDetailsCmd) <- ftrRepo.buildCreate(models)
    } yield {
    val bsCmds = bs.map(e => UpdateCommand(e, BankStatement.encode, POST_BANK_STATEMENT))
    executeBatchWithTx2(postgres, bsCmds, List(createTransactionCmd), List(createDetailsCmd))
    models.map(_.lines.size).sum + bs.size + models.length
  }

  def executeBatchWithTx2[A, B, C, D, E, F](postgres: Resource[Task, Session[Task]]
                                            , commands: List[UpdateCommand[E, F]]
                                            , insertCommands: List[InsertBatch[A, B]]
                                            , insertCommands2: List[InsertBatch[C, D]]
                                           ): Unit =
    postgres
      .use: session =>
        session.transaction.use: xa =>
          commands.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                xa.savepoint
                cmd.execute(command.encoder(command.param))).*>
          insertCommands.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                xa.savepoint
                cmd.execute(command.param.map(command.encoder))).*>
          insertCommands2.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                xa.savepoint
                cmd.execute(command.param.map(command.encoder)))
            .recoverWith:
              case SqlState.UniqueViolation(ex) =>
                ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                  xa.rollback
              case _ =>
                ZIO.logInfo(s"Error:  rolling back...") *>
                  xa.rollback

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
  
  val POST_BANK_STATEMENT: Command[Boolean *: Int *: Long *: Int *: String *: EmptyTuple] =
    sql"""UPDATE bankstatement SET
           posted                 = $bool,
           period                 = $int4,
          WHERE  id = $int8 AND modelid = $int4 AND company = $varchar
          """.command

  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM bankstatement WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command