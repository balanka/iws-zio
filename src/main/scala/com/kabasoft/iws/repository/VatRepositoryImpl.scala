package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Vat
import zio._
import com.kabasoft.iws.repository.Schema.vatSchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio.sql.ConnectionPool
import zio.stream._

final class VatRepositoryImpl(pool: ConnectionPool) extends VatRepository with IWSTableDescriptionPostgres {

  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  val vat = defineTable[Vat]("vat")

  val (
    id,
    name,
    description,
    percent,
    inputVatAccount,
    outputVatAccount,
    enterdate,
    changedate,
    postingdate,
    company,
    modelid
  ) = vat.columns

  val SELECT =
    select(id, name, description, percent, inputVatAccount, outputVatAccount, enterdate, changedate, postingdate, company, modelid).from(vat)

  override def create(c: Vat): ZIO[Any, RepositoryError, Unit]                         = {
    val query =
      insertInto(vat)(id, name, description, percent, inputVatAccount, outputVatAccount, enterdate, changedate, postingdate, company, modelid).values(
        Vat.unapply(c).get
      )

    ZIO.logDebug(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create(models: List[Vat]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(Vat.unapply(_).get)
    val query =
      insertInto(vat)(id, name, description, percent, inputVatAccount, outputVatAccount, enterdate, changedate, postingdate, company, modelid).values(
        data
      )
    ZIO.logDebug(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(vat).where(company === companyId && id === item))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getMessage))

  override def modify(model: Vat): ZIO[Any, RepositoryError, Int]        = {
    val update_ = build(model)
    ZIO.logDebug(s"Query Update vat is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getMessage))
  }
  override def modify(models: List[Vat]): ZIO[Any, RepositoryError, Int] = {
    val update_ = models.map(build)
    // ZIO.logDebug(s"Query Update vat is ${renderUpdate(update_)}") *>
    executeBatchUpdate(update_)
      .provideLayer(driverLayer).mapBoth(e => RepositoryError(e.getMessage), _.sum)
  }
  private def build(model: TYPE_)                                        =
    update(vat)
      .set(name, model.name)
      .set(description, model.description)
      .set(percent, model.percent)
      .set(inputVatAccount, model.inputVatAccount)
      .set(outputVatAccount, model.outputVatAccount)
      .where((id === model.id) && (company === model.company))

  override def all(companyId: String): ZIO[Any, RepositoryError, List[Vat]]                  =
    list(companyId).runCollect.map(_.toList)
  override def list(companyId: String): ZStream[Any, RepositoryError, Vat]                   = {
    val selectAll = SELECT

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Vat.apply _).tupled)).provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Vat]          = {
    val selectAll = SELECT.where((id === Id) && (company === companyId))

    ZIO.logDebug(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Vat.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Vat] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))

    ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Vat.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }
}

object VatRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, VatRepository] =
    ZLayer.fromFunction(new VatRepositoryImpl(_))
}
