package com.kabasoft.iws.repository
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.{ BankStatement, BankStatement_ }
import zio._
import zio.sql.ConnectionPool
import zio.stream._

final class BankStatementRepositoryImpl(pool: ConnectionPool) extends BankStatementRepository with BankStatementTableDescription {

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
    modelid
  ).from(bankStatements)

  override def create(bs: BankStatement): ZIO[Any, RepositoryError, Unit]              = {
    val query = insertInto(bankStatementInsert)(
      depositor_,
      postingdate_,
      valuedate_,
      postingtext_,
      purpose_,
      beneficiary_,
      accountno_,
      bankCode_,
      amount_,
      currency_,
      info_,
      company_,
      companyIban_,
      posted_,
      modelid_
    ).values(toTuple2(BankStatement_(bs)))

    ZIO.logDebug(s"Query to insert BankStatement is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create(models: List[BankStatement]): ZIO[Any, RepositoryError, Int]     = {
    val data  = models.map(c => toTuple2(BankStatement_(c)))
    val query = insertInto(bankStatementInsert)(
      depositor_,
      postingdate_,
      valuedate_,
      postingtext_,
      purpose_,
      beneficiary_,
      accountno_,
      bankCode_,
      amount_,
      currency_,
      info_,
      company_,
      companyIban_,
      posted_,
      modelid_
    ).values(data)

    ZIO.logDebug(s"Query to insert BankStatement is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(bankStatements).where((id === item.toLong) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: BankStatement): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bankStatements)
      .set(valuedate, model.valuedate)
      .set(accountno, model.accountno)
      .set(bankCode, model.bankCode)
      .set(posted, model.posted)
      .set(info, model.info)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query Update bankStatement is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[BankStatement]] =
    list(companyId).runCollect.map(_.toList)
  override def list(companyId: String): ZStream[Any, RepositoryError, BankStatement]  = {
    val selectAll = SELECT
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to(BankStatement.apply _)).provideDriver(driverLayer)
  }

  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, BankStatement]          = {
    val selectAll = SELECT.where((id === Id.toLong) && (company === companyId))
    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(BankStatement.apply _)).findFirstLong(driverLayer, Id.toLong)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, BankStatement] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to(BankStatement.apply _)).findFirstInt(driverLayer, modelId)
  }

}

object BankStatementRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, BankStatementRepository] =
    ZLayer.fromFunction(new BankStatementRepositoryImpl(_))
}
