package com.kabasoft.iws.repository

import com.kabasoft.iws.api.Protocol.bankAccountSchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import com.kabasoft.iws.domain.BankAccount
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class BankAccountRepositoryImpl(pool: ConnectionPool) extends BankAccountRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))
  val bankAccount = defineTable[BankAccount]("bankaccount")
  val (id, bic, owner, company, modelid) = bankAccount.columns
  val SELECT  = select(id, bic, owner, company, modelid).from(bankAccount)
  override def create(c: BankAccount): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(bankAccount)(id, bic, owner, company, modelid).values(BankAccount.unapply(c).get)

    ZIO.logDebug(s"Query to insert bank account is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[BankAccount]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(BankAccount.unapply(_).get)
    val query = insertInto(bankAccount)(id, bic, owner, company, modelid).values(data)

    ZIO.logDebug(s"Query to insert bank account is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(bankAccount).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: BankAccount): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bankAccount)
      .set(id, model.id)
      .set(bic, model.bic)
      .set(owner, model.owner)
      .set(company, model.company)
      .set(modelid, model.modelid)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query Update bank account is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[BankAccount]]                  =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, BankAccount]                   = {
    val selectAll = SELECT.where(company === companyId)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((BankAccount.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, BankAccount]          = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((BankAccount.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, BankAccount] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((BankAccount.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

}

object BankAccountRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, BankAccountRepository] =
    ZLayer.fromFunction(new BankAccountRepositoryImpl(_))
}
