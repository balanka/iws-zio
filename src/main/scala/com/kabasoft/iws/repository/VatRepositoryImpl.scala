package com.kabasoft.iws.repository

import zio._
import zio.stream._
import zio.sql.ConnectionPool
import com.kabasoft.iws.domain._
import com.kabasoft.iws.domain.AppError._

final class VatRepositoryImpl(pool: ConnectionPool) extends VatRepository with IWSTableDescriptionPostgres {
  import ColumnSet._

  val vat              =
    (string("id") ++ string("name") ++ string("description") ++ bigDecimal("percent") ++ string(
      "inputvataccount"
    ) ++ string("outputvataccount")
      ++ instant("enter_date") ++ instant("modified_date") ++ instant("posting_date") ++ string("company") ++ int(
        "modelid"
      ))
      .table("vat")

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
  val X                =
    id ++ name ++ description ++ percent ++ inputVatAccount ++ outputVatAccount ++ enterdate ++ changedate ++ postingdate ++ company ++ modelid
  lazy val driverLayer = ZLayer.make[SqlDriver](SqlDriver.live, ZLayer.succeed(pool))

  override def create(c: Vat): ZIO[Any, RepositoryError, Unit]                         = {
    val query = insertInto(vat)(X).values(Vat.unapply(c).get)

    ZIO.logInfo(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create(models: List[Vat]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(Vat.unapply(_).get)
    val query = insertInto(vat)(X).values(data)
    ZIO.logInfo(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(item: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(vat).where((id === item) && (company === companyId)))
      .provideLayer(driverLayer)
      .mapError(e => RepositoryError(e.getCause()))

  override def modify(model: Vat): ZIO[Any, RepositoryError, Int] = {
    val update_ = update(vat)
      .set(name, model.name)
      .set(description, model.description)
      .set(percent, model.percent)
      .set(inputVatAccount, model.inputVatAccount)
      .set(outputVatAccount, model.outputVatAccount)
      .where((id === model.id) && (company === model.company))
    ZIO.logInfo(s"Query Update vat is ${renderUpdate(update_)}") *>
      execute(update_)
        .provideLayer(driverLayer)
        .mapError(e => RepositoryError(e.getCause()))
  }

  override def list(companyId: String): ZStream[Any, RepositoryError, Vat]                   = {
    val selectAll = select(X).from(vat)

    ZStream.fromZIO(
      ZIO.logInfo(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Vat.apply _).tupled)).provideDriver(driverLayer)
  }
  override def getBy(Id: String, companyId: String): ZIO[Any, RepositoryError, Vat]          = {
    val selectAll = select(X).from(vat).where((id === Id) && (company === companyId))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Vat.apply _).tupled))
        .findFirst(driverLayer, Id)
  }
  override def getByModelId(modelId: Int, companyId: String): ZIO[Any, RepositoryError, Vat] = {
    val selectAll = select(X).from(vat).where((modelid === modelId) && (company === companyId))

    ZIO.logInfo(s"Query to execute getByModelId is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Vat.apply _).tupled))
        .findFirstInt(driverLayer, modelId)
  }
}

object VatRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, VatRepository] =
    ZLayer.fromFunction(new VatRepositoryImpl(_))
}
