package com.kabasoft.iws.repository

import zio._
import zio.stream._
import zio.sql.ConnectionPool
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError._

final class BankStatementRepositoryImpl(pool: ConnectionPool)
    extends BankStatementRepository
    with BankStatementTableDescription {

  lazy val driverLayer = ZLayer
    .make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val X  =
    id ++ depositor ++ postingdate ++ valuedate ++ postingtext ++ purpose ++ beneficiary ++ accountno ++ bankCode ++ amount ++ currency ++ info ++ company ++ companyIban ++ posted ++ modelid
  val X2 =
    depositor_ ++ postingdate_ ++ valuedate_ ++ postingtext_ ++ purpose_ ++ beneficiary_ ++ accountno_ ++ bankCode_ ++ amount_ ++ currency_ ++ info_ ++ company_ ++ companyIban_ ++ posted_ ++ modelid_

  override def create(i: BankStatement): ZIO[Any, RepositoryError, Unit]               = {
    val query = insertInto(bankStatementInsert)(X2).values(toTuple2(i))

    ZIO.logInfo(s"Query to insert BankStatement is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create(models: List[BankStatement]): ZIO[Any, RepositoryError, Int]     = {
    val data  = models.map(c => toTuple2(c))
    val query = insertInto(bankStatementInsert)(X2).values(data)

    ZIO.logInfo(s"Query to insert BankStatement is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(bankStatements).where((id === item.toLong) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: BankStatement): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bankStatements)
      .set(valuedate, model.valuedate)
      .set(accountno, model.accountno)
      .set(bankCode, model.bankCode)
      .set(info, model.info)
      .where((id === model.id) && (company === model.company))
    ZIO.logInfo(s"Query Update bankStatement is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, BankStatement]                   = {
    val selectAll = select(X).from(bankStatements)
    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *> execute(selectAll.to(BankStatement.apply _)).provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, BankStatement]          = {
    val selectAll = select(X)
      .from(bankStatements)
      .where((id === Id.toLong) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to(BankStatement.apply _)).findFirstLong(driverLayer, Id.toLong)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, BankStatement] = {
    val selectAll = select(X)
      .from(bankStatements)
      .where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to(BankStatement.apply _)).findFirstInt(driverLayer, modelId)
  }
  /*
  override def findById(id: String): ZIO[Any, RepositoryError, BankStatement] = {
    val selectAll = select(bid++depositor++postingdate++valuedate++postingtext++purpose++beneficiary++accountno++
      bankCode++amount++currency++info++company++companyIban++modelid)
      .from(bankStatements)
      .where(bid === id)

    ZIO.logInfo(s"Query to execute findById is ${renderRead(selectAll)}") *>
      execute(selectAll.to((BankStatement.apply _).tupled))
        .findFirstLong(driverLayer, id.toLong)
  }


  override def update(model: BankStatement, companyId: String): ZIO[Any, RepositoryError, Int]={
    val update=
        update(bankStatementsTable)
      .set(amount, model.amount)
      .set(bankCode, model.bankCode)
      .where(bid === model.bid)//, company ==model.company)

     ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute update is ${renderRead(update)}")
    ) *>
      execute(update)
        .provideDriver(driverLayer)
  }

  override def findSome(companyId: String, param: String*): ZStream[Any, RepositoryError, BankStatement]={
    val selectAll =
      select( bid++depositor++postingdate++valuedate++postingtext++purpose++beneficiary++accountno++
                 bankCode++amount++currency++info++company++companyIban)
                 .from(bankStatements)
                  .where(company === companyId)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findSome is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((BankStatement.apply _).tupled))
        .provideDriver(driverLayer)

  }
   */
}

object BankStatementRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, BankStatementRepository] =
    ZLayer.fromFunction(new BankStatementRepositoryImpl(_))
}
