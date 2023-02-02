package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Bank
import com.kabasoft.iws.api.Protocol.bankschema
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio._
import zio.sql.ConnectionPool
import zio.stream._
final class BankRepositoryImpl(pool: ConnectionPool) extends BankRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val bank = defineTable[Bank]("bank")

  val (id, name, description, enterdate, changedate, postingdate, modelid, company)    = bank.columns

  val SELECT                                                                           = select(id, name, description, enterdate, changedate, postingdate, modelid, company).from(bank)
  override def create(c: Bank): ZIO[Any, RepositoryError, Unit]                        = {
    val query = insertInto(bank)(id,
      name,
      description,
      enterdate,
      changedate,
      postingdate,
      modelid,
      company).values(Bank.unapply(c).get)

    ZIO.logDebug(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
        .unit
  }
  override def create(models: List[Bank]): ZIO[Any, RepositoryError, Int]              = {
    val data  = models.map(Bank.unapply(_).get)
    val query = insertInto(bank)(id,
      name,
      description,
      enterdate,
      changedate,
      postingdate,
      modelid,
      company).values(data)

    ZIO.logDebug(s"Query to insert Bank is ${renderInsert(query)}") *>
      execute(query)
        .provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(bank).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Bank): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(bank)
      .set(name, model.name)
      .set(description, model.description)
      .where((id === model.id) && (company === model.company))
    ZIO.logDebug(s"Query Update Bank is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Bank]]                  =
    list(companyId).runCollect.map(_.toList)

  override def list(companyId: String): ZStream[Any, RepositoryError, Bank]                   = {
    val selectAll = SELECT.where(company === companyId)
    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Bank.apply _).tupled))
        .provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Bank]          = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Bank.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Bank] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Bank.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }

}

object BankRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, BankRepository] =
    ZLayer.fromFunction(new BankRepositoryImpl(_))
}
