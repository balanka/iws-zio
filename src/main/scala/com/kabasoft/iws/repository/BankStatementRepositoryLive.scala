package com.kabasoft.iws.repository
import cats.*
import cats.effect.Resource
import cats.syntax.all.*
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankStatement, common, FinancialsTransaction, FinancialsTransactionDetails}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import zio.prelude.FlipOps
import zio.interop.catz.*
import zio.stream.interop.fs2z.*
import zio.{Task, ZIO, ZLayer}
import java.time.{Instant, LocalDateTime, ZoneId}

final case  class BankStatementRepositoryLive(postgres: Resource[Task, Session[Task]]
                                              , ftrRepo: FinancialsTransactionRepository)
  extends BankStatementRepository, MasterfileCRUD:

  import BankStatementRepositorySQL.*

  def buildId(transactions: List[FinancialsTransaction]): List[FinancialsTransaction] =
    transactions.zipWithIndex.map { case (ftr, i) =>
    val idx = Instant.now().getNano + i.toLong
    ftr.copy(id1 = idx, lines = ftr.lines.map(_.copy(transid = idx)), period = common.getPeriod(ftr.transdate))
  }
    
  private def transact(s: Session[Task], models: List[FinancialsTransaction], oldmodels: List[BankStatement]): Task[Unit] =
    s.transaction.use: xa =>
      s.prepareR(FinancialsTransactionRepositorySQL.insert).use: pciMaster =>
        s.prepareR(UPDATE).use: pcuMaster =>
          s.prepareR(FinancialsTransactionRepositorySQL.insertDetails).use: pciDetails =>
            s.prepareR(FinancialsTransactionRepositorySQL.UPDATE_DETAILS).use: pcuDetails =>
              tryExec(xa, pciMaster, pciDetails, pcuMaster, pcuDetails
                , models, models.flatMap(_.lines).map(FinancialsTransactionDetails.encodeIt4)
                , oldmodels.map(BankStatement.encodeIt2), List.empty)
              
  override def create(c: BankStatement):ZIO[Any, RepositoryError, Int] = create(List(c))
  override def create(list: List[BankStatement]):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, 
    list.map(BankStatement.encodeIt4), insertAll(list.size), list.size)
  override def modify(model: BankStatement):ZIO[Any, RepositoryError, Int]= executeWithTx(postgres, model, BankStatement.encodeIt2, UPDATE, 1)
  override def modify(models: List[BankStatement]):ZIO[Any, RepositoryError, Int] = executeBatchWithTxK(postgres, models, UPDATE, BankStatement.encodeIt2)
  override def all(p: (Int, String)): ZIO[Any, RepositoryError, List[BankStatement]] = queryWithTx(postgres, p, ALL)
  override def getById(p: (Long, Int, String)): ZIO[Any, RepositoryError, BankStatement] = queryWithTxUnique(postgres, p, BY_ID)
  override def getBy(ids: List[Long], modelid: Int, company: String): ZIO[Any, RepositoryError, List[BankStatement]] =
    queryWithTx(postgres, (ids, modelid, company), ALL_BY_ID(ids.length))

  def delete(p: (Long, Int, String)):ZIO[Any, RepositoryError, Int]=  executeWithTx(postgres, p, DELETE, 1)
  override def deleteAll(): ZIO[Any, RepositoryError, Int] =
    (postgres
      .use:
        session =>
          session.execute(DELETE_All)
      .mapBoth(e => RepositoryError(e.getMessage), _ => 1))
  
  override def post(bs: List[BankStatement], models: List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] =
    postgres
      .use: session =>
          transact(session, buildId(models), bs)
      .mapBoth(e => RepositoryError(e.getMessage), _ => models.flatMap(_.lines).size + models.size )  

object BankStatementRepositoryLive:
  val live: ZLayer[Resource[Task, Session[Task]] & FinancialsTransactionRepository, Throwable, BankStatementRepository] =
    ZLayer.fromFunction(new BankStatementRepositoryLive(_, _))

private[repository] object BankStatementRepositorySQL:

  private[repository] def toInstant(localDateTime: LocalDateTime): Instant =
    localDateTime.atZone(ZoneId.of("Europe/Paris")).toInstant

  private val bankStatementCodec =
    (int8 *:varchar *: timestamp *: timestamp *: varchar *: varchar *: varchar *: varchar *: varchar *: numeric(12, 2) *: varchar *: varchar *: varchar *: varchar *: bool *: int4 *: int4)

  private val bankStatementCodec4 =
    (varchar *: timestamp *: timestamp *: varchar *: varchar *: varchar *: varchar *: varchar *: numeric(12,2) *: varchar *: varchar *: varchar *: varchar *: bool *: int4 *:  int4 )

  val mfDecoder: Decoder[BankStatement] = bankStatementCodec.map:
    case (id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bankCode, amount, currency, info, company, companyIban, posted, modelid, period) =>
      BankStatement(id, depositor, toInstant(postingdate), toInstant(valuedate), postingtext, purpose, beneficiary, accountno, bankCode, amount.bigDecimal, currency, info, company, companyIban, posted, modelid, period) 
  
  val mfEncoder: Encoder[BankStatement] = bankStatementCodec4.values.contramap(BankStatement.encodeIt4)
  
  def base =
    sql""" SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement """

  def ALL_BY_ID(nr: Int): Query[(List[Long], Int, String), BankStatement] =
    sql"""
           SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement
           WHERE id  IN (${int8.list(nr)}) AND  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val BY_ID: Query[Long *: Int *: String *: EmptyTuple, BankStatement] =
    sql"""
           SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount, currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement
           WHERE id = $int8 AND modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val ALL: Query[Int *: String *: EmptyTuple, BankStatement] =
    sql""" SELECT id, depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno, bank_code, amount
           , currency, info, company, company_iban, posted, modelid, period
           FROM   bankstatement
           WHERE  modelid = $int4 AND company = $varchar
           """.query(mfDecoder)

  val insert: Command[BankStatement] =
    sql"""INSERT INTO bankstatement (depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno
         , bank_code, amount, currency, info, company, company_iban, posted, modelid, period) 
          VALUES $mfEncoder """.command

  def insertAll(n:Int): Command[List[BankStatement.TYPE4]] =
    sql"""INSERT INTO bankstatement (depositor, postingdate, valuedate, postingtext, purpose, beneficiary, accountno
         , bank_code, amount, currency, info, company, company_iban, posted, modelid, period) 
         VALUES ${bankStatementCodec4.values.list(n)}""".stripMargin.command

  val UPDATE: Command[BankStatement.TYPE3] =
    sql"""UPDATE bankstatement SET valuedate=$timestamp, accountno= $varchar
          , bank_code= $varchar, period= $int4, posted = $bool  
          WHERE id =$int8 and modelid =$int4 and company = $varchar""".command
  
  def DELETE: Command[(Long, Int, String)] =
    sql"DELETE FROM bankstatement WHERE id = $int8 AND modelid = $int4 AND company = $varchar".command

  def DELETE_All: Command[Void] = sql"DELETE FROM bankstatement WHERE  company = '-1000'".command   