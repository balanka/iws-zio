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

  private def buildInsertQuery(bs: List[BankStatement]) =
    insertInto(bankStatementInsert)(
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
    ).values(bs.map(BankStatement_.apply).map(toTuple2))

  private def  buildInsertQuery(bs: BankStatement) =
    insertInto(bankStatementInsert)(
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
  override def create(bs: BankStatement): ZIO[Any, RepositoryError, BankStatement]              =
    create2(bs)*>getById(bs.id)
  override def create2(bs: BankStatement): ZIO[Any, RepositoryError, Unit]              = {
    val query = buildInsertQuery(bs)
    ZIO.logDebug(s"Query to insert BankStatement is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }

  override def create(models: List[BankStatement]): ZIO[Any, RepositoryError, List[BankStatement]]     = {
    create2(models)*>getById(models.map(_.id)).runCollect.map(_.toList)
  }
  override def create2(models: List[BankStatement]): ZIO[Any, RepositoryError, Int]     = {
    val query = buildInsertQuery(models)
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

  override def modify(model: BankStatement): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bankStatements)
      .set(valuedate, model.valuedate)
      .set(accountno, model.accountno)
      .set(bankCode, model.bankCode)
      .set(posted, model.posted)
      .set(info, model.info)
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
    val selectAll = SELECT
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

}

object BankStatementRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, BankStatementRepository] =
    ZLayer.fromFunction(new BankStatementRepositoryImpl(_))
}
