package com.kabasoft.iws.repository
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{BankStatement, BankStatement_, FinancialsTransaction, common}
import zio._
import zio.prelude.FlipOps
import zio.sql.ConnectionPool
import zio.stream._

import java.time.Instant
import scala.annotation.nowarn

final class BankStatementRepositoryImpl(pool: ConnectionPool, accRepo: AccountRepository ) extends BankStatementRepository with BankStatementTableDescription
with TransactionTableDescription {
type TYPE = (TableName, Instant, Instant, TableName, TableName, TableName, TableName, TableName, java.math.BigDecimal, TableName, TableName, TableName, TableName, Boolean, Int, Int)
  lazy val driverLayer = ZLayer
    .make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val SELECT = select(
    id,
    depositor,
    postingdate,
    valuedate,
    postingtext,
    purpose,
    beneficiary,
    accountno,
    bankCode,
    amount,
    currency,
    info,
    company,
    companyIban,
    posted,
    modelid,
    period
  ).from(bankStatements)

  //private def buildInsertQuery(bs: BankStatement): Insert[BankStatement_,  TYPE]  = buildInsertQuery(List(bs))
  private def  buildInsertQueryBS(bs: List[BankStatement]) =
    insertInto(bankStatementInsert)(
      depositor_bs,
      postingdatex_bs,
      valuedatex_bs,
      postingtextx_bs,
      purposex_bs,
      beneficiaryx_bs,
      accountnox_bs,
      bankCodex_bs,
      amountx_bs,
      currencyx_bs,
      infox_bs,
      companyx_bs,
      companyIbanx_bs,
      postedx_bs,
      modelidx_bs,
      periodx_bs
    ).values(bs.map(BankStatement_.apply).map(toTuple2))
  override def create(bs: BankStatement): ZIO[Any, RepositoryError, BankStatement]              =
    create2(bs)*>getById(bs.id)
  override def create2(bs: BankStatement): ZIO[Any, RepositoryError, Unit]              = {
    val query = buildInsertQueryBS(List(bs))
    ZIO.logDebug(s"Query to insert BankStatement is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }

  override def create(models: List[BankStatement]): ZIO[Any, RepositoryError, List[BankStatement]]     = {
    create2(models)*>getById(models.map(_.id)).runCollect.map(_.toList)
  }
  override def create2(models: List[BankStatement]): ZIO[Any, RepositoryError, Int]     = {
    val query = buildInsertQueryBS(models)
    ZIO.logDebug(s"Query to insert BankStatement is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }

  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] = {
    val delete_ = deleteFrom(bankStatements).where((company === companyId) && id === item.toLong)
    ZIO.logDebug(s"Delete Bank is ${renderDelete(delete_)}") *>
      execute(delete_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  private def buildUpdate(model:BankStatement): Update[BS] =
    update(bankStatements)
      .set(posted, true)
      .set(period, common.getPeriod(model.valuedate))
      .where((id === model.id) && (company === model.company))


  override def modify(model: BankStatement): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bankStatements)
      .set(valuedate, model.valuedate)
      .set(accountno, model.accountno)
      .set(bankCode, model.bankCode)
      .set(posted, model.posted)
      .set(info, model.info)
      .set(period, common.getPeriod(model.valuedate))
      .where((id === model.id) && (company === model.company))
    ZIO.logInfo(s"Query Update bankStatement is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }



  override def update(model: BankStatement): ZIO[Any, RepositoryError, BankStatement] =
    if (model.id <= 0) {
      create(model)
    } else {
      modify(model) *> getById(model.id)
    }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[BankStatement]] =
    list(companyId).runCollect.map(_.toList)
  override def list(companyId: String): ZStream[Any, RepositoryError, BankStatement]  = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to(BankStatement.apply )).provideDriver(driverLayer)
  }

  override def getById(Id: Long): ZIO[Any, RepositoryError, BankStatement] = {
    val selectAll = SELECT.where(id === Id)
    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(BankStatement.apply)).findFirstLong(driverLayer, Id.toLong)
  }

  override def getById(Ids: List[Long]): ZStream[Any, RepositoryError, BankStatement] = {
    val selectAll = SELECT.where(id in Ids)
    ZStream.fromZIO(
    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}")) *>
      execute(selectAll.to(BankStatement.apply))
        .provideDriver(driverLayer)
  }

  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, BankStatement]          = {
    val selectAll = SELECT.where((id === Id.toLong) && (company === companyId))
    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(BankStatement.apply )).findFirstLong(driverLayer, Id.toLong)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, BankStatement] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to(BankStatement.apply )).findFirstInt(driverLayer, modelId)
  }

  @nowarn
  override def post(bs: List[BankStatement], transactions:List[FinancialsTransaction]): ZIO[Any, RepositoryError, Int] = {
    val updateSQL = bs.map(buildUpdate)
    var company:String =null
    val ids = transactions.flatMap(tr=>{ company = tr.company; tr.lines.map(_.account)})++transactions.flatMap(tr=>tr.lines.map(_.oaccount))
    val result = for {
      accounts        <-  accRepo.getBy(ids, company)
      posted <- ZIO.logInfo(s"Update stmt bank statement  ${updateSQL.map(renderUpdate)}  ") *>updateSQL.map(_.run).flip.map(_.sum)
      created <- ZIO.logInfo(s"Posted bank statement  ${posted}  ") *> create2s(transactions, accounts)
      _<- ZIO.logInfo(s"Created transactions  ${posted}  ")
    } yield posted+created
    transact(result)
      .mapError(e => RepositoryError(e.toString))
      .provideLayer(driverLayer)
  }



}

object BankStatementRepositoryImpl {
  val live: ZLayer[ConnectionPool with FinancialsTransactionRepository with AccountRepository, Throwable, BankStatementRepository] =
    ZLayer.fromFunction(new BankStatementRepositoryImpl(_, _))
}
