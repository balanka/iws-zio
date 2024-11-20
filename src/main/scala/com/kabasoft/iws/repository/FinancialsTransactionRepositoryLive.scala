package com.kabasoft.iws.repository

import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{FinancialsTransaction, FinancialsTransactionDetails}
import com.kabasoft.iws.repository.MasterfileCRUD.{UpdateCommand, InsertBatch, ExecCommand}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.interop.catz.*
import zio.{ZIO, *}

import java.time.{Instant, LocalDateTime, ZoneId}


final case  class FinancialsTransactionRepositoryLive(postgres: Resource[Task, Session[Task]]
                                                      , accRepo: AccountRepository) extends FinancialsTransactionRepository, MasterfileCRUD:

  import FinancialsTransactionRepositorySQL._
  
  def getId: ZIO[Any, RepositoryError, Long] = for {
    idx <- queryWithTxUnique(postgres, TRANS_ID)
  } yield idx

  private def setTransId(c: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    idx <- queryWithTxUnique(postgres, TRANS_ID)
  } yield c.copy(id = idx, lines = c.lines.map(l => l.copy(transid = idx)))
  
  override def create(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = create(List(model))
   def buildTransaction(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
     result <- ZIO.collectAll(models.map(setTransId))
  } yield result

  override def buildCreate(models: List[FinancialsTransaction]):
  ZIO[Any, RepositoryError, (InsertBatch[FinancialsTransaction,FinancialsTransaction.TYPE]
                          , InsertBatch[FinancialsTransactionDetails, FinancialsTransactionDetails.D_TYPE])] = for {
    transactions <- buildTransaction(models)
  } yield {
    val newLines = transactions.flatMap(_.lines)
    val insertTransactionCmd = FinancialsTransactionRepositorySQL.insertAll(transactions.length)
    val insertDetailsCmd = FinancialsTransactionRepositorySQL.insertAllDetails(newLines.length)
    val createDetailsCmd = InsertBatch(newLines, FinancialsTransactionDetails.encodeIt, insertDetailsCmd)
    val createTransactionCmd = InsertBatch(transactions, FinancialsTransaction.encodeIt, insertTransactionCmd)
    (createTransactionCmd, createDetailsCmd)
  }

  override def create(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = for {
    (createTransactionCmd, createDetailsCmd) <-  buildCreate(models)
  } yield  {
    executeBatchWithTx2(postgres, List.empty, List(createTransactionCmd), List.empty, List(createDetailsCmd), List.empty)
    models.map(_.lines.size).sum + models.size
  }

  override def modify(model: FinancialsTransaction): ZIO[Any, RepositoryError, Int] = modify(List(model))
  override def modify(models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
  {
    val newLines = models.flatMap(ftr=>ftr.lines.filter(_.id == -1L).map(l => l.copy(transid = ftr.id)))
    val deletedLine = models.flatMap(ftr=>ftr.lines.filter(_.transid == -2L))
    val deletedLineIds = deletedLine.map(line => line.id)
    val oldLines2Update = models.flatMap(ftr=>ftr.lines.filter(_.id > 0L).map(l => l.copy(transid = ftr.id)))
    val insertDetailsCmd = FinancialsTransactionRepositorySQL.insertAllDetails(newLines.length)
    val updateDetailsCmd = FinancialsTransactionRepositorySQL.UPDATE_DETAILS
    val createDetailsCmd = InsertBatch(newLines, FinancialsTransactionDetails.encodeIt, insertDetailsCmd)
    val updateDetailsCmds = ExecCommand(oldLines2Update, FinancialsTransactionDetails.encodeIt2, updateDetailsCmd)
    val deleteDetailsCmd = ExecCommand(oldLines2Update, FinancialsTransactionDetails.encodeIt3, DELETE_DETAILS)
    val updateFtrCmd = models.map(ftr=>UpdateCommand(ftr, FinancialsTransaction.encodeIt2, FinancialsTransactionRepositorySQL.UPDATE))
    executeBatchWithTx2(postgres, updateFtrCmd, List.empty, List(deleteDetailsCmd), List(createDetailsCmd), List(updateDetailsCmds))
    ZIO.succeed(newLines.size + oldLines2Update.size + deletedLine.size + 1)
  }
  
  def list(p: (Int, String)):ZIO[Any, RepositoryError, List[FinancialsTransaction]] = queryWithTx(postgres, p, ALL)

  private def  getDetails(p:(Long, String)): ZIO[Any, RepositoryError, List[FinancialsTransactionDetails]] = for {
    details <- queryWithTx(postgres, p, DETAILS1)
  }yield details

  private def getByTransId1(trans: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] = for {
    lines_ <- getDetails(trans.id1, trans.company)
  } yield trans.copy(lines = if (lines_.nonEmpty) lines_ else List.empty[FinancialsTransactionDetails])

  private def withLines(trans: FinancialsTransaction): ZIO[Any, RepositoryError, FinancialsTransaction] =
    getByTransId1(trans)

  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] = for {
    transactions <- list(p)
    details <- transactions.map(withLines).flip
  } yield details

  override def getById(p: (Long, Int, String)):ZIO[Any, RepositoryError, FinancialsTransaction] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Long],  modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))
  override def getByIds(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, (ids, modelid, company), BY_IDS(ids.length))
  override def getByModelId(modelid: (Int, String)): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, modelid, BY_MODEL_ID)
  override def getByTransId(p: (Long, String)): ZIO[Any, RepositoryError, FinancialsTransaction] =
    queryWithTxUnique(postgres, p, BY_TRANS_ID)

  def find4Period(fromPeriod: Int, toPeriod: Int, modelid:Int, companyId: String, posted:Boolean): ZIO[Any, RepositoryError, List[FinancialsTransaction]] =
    queryWithTx(postgres, (modelid, companyId, posted, fromPeriod, toPeriod), FIND_4_PERIOD)
  def delete(p: (Long, Int, String)): ZIO[Any, RepositoryError, Int] = executeWithTx(postgres, p, DELETE, 1)

  def executeBatchWithTx2[A, B, C, D, E, F](postgres: Resource[Task, Session[Task]]
                                            , commands: List[UpdateCommand[A, B]]
                                            , insertCommands: List[InsertBatch[A, B]]
                                            , deleteCommands: List[ExecCommand[C, F]]
                                            , insertCommands2: List[InsertBatch[C, D]]
                                            , commandLPs: List[ExecCommand[C, E]],
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
          deleteCommands.traverse(command =>
            session
              .prepare(command.cmd)
              .flatMap: cmd =>
                xa.savepoint
                command.param.traverse(p =>
                  cmd.execute(command.encoder(p)))).*>
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
                cmd.execute(command.param.map(command.encoder))).*>
          commandLPs.traverse(command =>
              session
                .prepare(command.cmd)
                .flatMap: cmd =>
                  xa.savepoint
                  command.param.traverse(p =>
                    cmd.execute(command.encoder(p))))
            .recoverWith:
              case SqlState.UniqueViolation(ex) =>
                ZIO.logInfo(s"Unique violation: ${ex.constraintName.getOrElse("<unknown>")}, rolling back...") *>
                  xa.rollback
              case _ =>
                ZIO.logInfo(s"Error:  rolling back...") *>
                  xa.rollback
                
object FinancialsTransactionRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & AccountRepository, Throwable, FinancialsTransactionRepository] =
    ZLayer.fromFunction(new FinancialsTransactionRepositoryLive(_, _))

object FinancialsTransactionRepositorySQL:
  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val financialsTransactionCodec =
    (int8 *: int8 *: int8 *: varchar *: varchar *: timestamp *: timestamp *: timestamp *: int4 *: bool *: int4 *: varchar *: varchar *: int4 *: int4)

  private val financialsDetailsTransactionCodec =
    (int8 *: int8 *: varchar *: bool *: varchar *: numeric(12,2) *: timestamp *: varchar *: varchar *: varchar *: varchar *: varchar)

  val mfDecoder: Decoder[FinancialsTransaction] = financialsTransactionCodec.map:
    case (id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content) =>
      FinancialsTransaction(id, oid, id1, costcenter, account, toInstant(transdate), toInstant(enterdate)
        , toInstant(postingdate), period, posted, modelid, company, text, type_journal, file_content)


  val mfEncoder: Encoder[FinancialsTransaction] = financialsTransactionCodec.values.contramap(FinancialsTransaction.encodeIt)
  val detailsEncoder: Encoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.values.contramap(FinancialsTransactionDetails.encodeIt)

  def detailsDecoder: Decoder[FinancialsTransactionDetails] = financialsDetailsTransactionCodec.map:
      case (id, transid, account, side, oaccount, amount, duedate, text, currency, company, accountName, oaccountName) =>
        FinancialsTransactionDetails(id, transid, account, side, oaccount, amount.bigDecimal, toInstant(duedate), text
          , currency,  company, accountName, oaccountName)

  def base =
    sql""" SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta """

  val TRANS_ID:Query[Void, Long] = sql"""SELECT NEXTVAL('master_compta_id_seq')""".query(int8)
  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id  IN ${int8.list(nr)} AND modelid= $int4 AND company = $varchar
           """.query(mfDecoder)

  def BY_IDS(nr: Int): Query[(List[Long], Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id  IN ${int8.list(nr)} AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  def BY_MODEL_ID: Query[(Int, String), FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE modelid= $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID1: Query[Long *: Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id1 = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""
           SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  def ALL_DETAILS_ID(lst: List[Long], company:String) = {
    val query: Fragment[Void] =
      sql"""SELECT id, transid, account, side, oaccount, amount,  duedate, text, currency,  company, account_name, oaccount_name
           FROM   details_compta"""
    query(Void) |+|
      AppliedFragment.apply[lst.type](sql" WHERE transid  IN (${int8.list(lst)})", lst) |+|
      sql""" AND company = $varchar""".apply(company)
  }

  def DETAILS(n: Int): Query[List[Long] *: String *: EmptyTuple, FinancialsTransactionDetails] =
    sql"""SELECT id, transid, account, side, oaccount, amount,  duedate, text, currency,  company, account_name, oaccount_name
           FROM   details_compta
           WHERE  transid  in ${int8.list(n)}  AND company = $varchar
           """.query(detailsDecoder)

  val DETAILS1: Query[Long *: String *: EmptyTuple, FinancialsTransactionDetails] =
    sql"""SELECT id, transid, account, side, oaccount, amount,  duedate, text, currency,  company, account_name, oaccount_name
           FROM   details_compta
           WHERE  transid = $int8  AND company = $varchar
           """.query(detailsDecoder)

  val FIND_4_PERIOD: Query[Int *: String *: Boolean *: Int *: Int *: EmptyTuple, FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE modelid= $int4 AND company = $varchar AND posted =$bool AND period between $int4  AND $int4
           """.query(mfDecoder)

  val BY_TRANS_ID: Query[Long *: String *: EmptyTuple, FinancialsTransaction] =
    sql"""SELECT id, oid, id1, costcenter, account, transdate, enterdate, postingdate, period, posted, modelid, company, text, type_journal, file_content
           FROM   master_compta
           WHERE id= $int8 AND company = $varchar 
           """.query(mfDecoder)  

  val insert: Command[FinancialsTransaction] = sql"""INSERT INTO master_compta VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[FinancialsTransaction.TYPE]] =
    sql"INSERT INTO master_compta VALUES ${financialsTransactionCodec.values.list(n)}".command

  def insertAllDetails(n:Int): Command[List[FinancialsTransactionDetails.D_TYPE]] =
    sql"INSERT INTO details_compta VALUES ${financialsDetailsTransactionCodec.values.list(n)}".command

  val UPDATE: Command[FinancialsTransaction.TYPE2] =
    sql"""UPDATE master_compta
          SET oid = $int8, costcenter = $varchar, account = $varchar, text=$varchar, type_journal = $int4, file_content = $int4
          WHERE id=$int8 and modelid=$int4 and company= $varchar""".command

  val UPDATE_DETAILS: Command[FinancialsTransactionDetails.TYPE2] =
    sql"""UPDATE details_compta
          SET account = $varchar, side = $bool, oaccount = $varchar, amount = $numeric, duedate = $timestamp, text=$varchar, currency = $varchar
          , account_name= $varchar, oaccount_name= $varchar
          WHERE id=$int8 and company= $varchar""".command

  val updatePosted: Command[Long *: Int *: String *: EmptyTuple] =
    sql"""UPDATE master_compta UPDATE SET posted = true
            WHERE id =$int8 AND modelid = $int4 AND  company =$varchar and posted=false
          """.command

  private val onConflictDoNothing = sql"ON CONFLICT DO NOTHING"

  val DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM master_compta WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command
    
  val DELETE_DETAILS: Command[(Long, String)] = sql"DELETE FROM details_compta WHERE id = $int8 AND company = $varchar".command
  