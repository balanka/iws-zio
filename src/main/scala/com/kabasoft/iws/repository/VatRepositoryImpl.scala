package com.kabasoft.iws.repository

import com.kabasoft.iws.domain.Vat
import zio._
import com.kabasoft.iws.repository.Schema.vatSchema
import com.kabasoft.iws.domain.AppError.RepositoryError
import zio.prelude.FlipOps
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

  def whereClause(Id: String, companyId: String) =
    List(id === Id, company === companyId)
      .fold(Expr.literal(true))(_ && _)

  def whereClause(Ids: List[String], companyId: String) =
    List(company === companyId, id in Ids).fold(Expr.literal(true))(_ && _)

  override def create(c: Vat): ZIO[Any, RepositoryError, Vat] = create2(c) *> getBy((c.id, c.company))

  override def create(models: List[Vat]): ZIO[Any, RepositoryError, List[Vat]] =
    if (models.isEmpty) {
      ZIO.succeed(List.empty[Vat])
    } else {
      create2(models) *> getBy(models.map(_.id), models.head.company)
    }
  override def create2(c: Vat): ZIO[Any, RepositoryError, Unit]                         = {
    val query =
      insertInto(vat)(id, name, description, percent, inputVatAccount, outputVatAccount, enterdate, changedate, postingdate, company, modelid).values(
        Vat.unapply(c).get
      )

    ZIO.logDebug(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer).unit
  }
  override def create2(models: List[Vat]): ZIO[Any, RepositoryError, Int]               = {
    val data  = models.map(Vat.unapply(_).get)
    val query =
      insertInto(vat)(id, name, description, percent, inputVatAccount, outputVatAccount, enterdate, changedate, postingdate, company, modelid).values(
        data
      )
    ZIO.logDebug(s"Query to insert Vat is ${renderInsert(query)}") *>
      execute(query).provideAndLog(driverLayer)
  }
  override def delete(idx: String, companyId: String): ZIO[Any, RepositoryError, Int] =
    execute(deleteFrom(vat).where((company === companyId) && (id === idx) ))
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
  private def build(model: Vat)                                        =
    update(vat)
      .set(name, model.name)
      .set(description, model.description)
      .set(percent, model.percent)
      .set(inputVatAccount, model.inputVatAccount)
      .set(outputVatAccount, model.outputVatAccount)
      .where(whereClause(model.id, model.company))

  override def all(Id:(Int, String)): ZIO[Any, RepositoryError, List[Vat]]                  =
    list(Id).runCollect.map(_.toList)
  override def list(Id:(Int, String)): ZStream[Any, RepositoryError, Vat]                   = {
    val selectAll = SELECT.where(modelid === Id._1 && company === Id._2)

    ZStream.fromZIO(
      ZIO.logDebug(s"Query to execute findAll is ${renderRead(selectAll)}")
    ) *>
      execute(selectAll.to((Vat.apply _).tupled)).provideDriver(driverLayer)
  }
  override def getBy(Id:(String, String)): ZIO[Any, RepositoryError, Vat]          = {
    val selectAll = SELECT.where(whereClause(Id._1, Id._2))

    ZIO.logInfo(s"Query to execute findBy is ${renderRead(selectAll)}") *>
      execute(selectAll.to((Vat.apply _).tupled))
        .findFirst(driverLayer, Id._1)
  }

  def getBy(ids: List[String], company: String): ZIO[Any, RepositoryError, List[Vat]] =
    ids.map(id=>getBy((id, company))).flip
//    for {
//    vats <- getBy_(ids, company).runCollect.map(_.toList)
//  } yield vats

  def getBy_(ids: List[String], company: String): ZStream[Any, RepositoryError, Vat] = {
    val selectAll = SELECT.where(whereClause(ids, company))
    execute(selectAll.to((Vat.apply _).tupled))
      .provideDriver(driverLayer)
  }
  override def getByModelId(id: (Int, String)): ZIO[Any, RepositoryError, List[Vat]] = for {
    all <- getByModelIdStream(id._1, id._2).runCollect.map(_.toList)
  } yield all

  override def getByModelIdStream(modelId: Int, companyId: String): ZStream[Any, RepositoryError, Vat] = {
    val selectAll = SELECT.where((modelid === modelId) && (company === companyId))
    ZStream.fromZIO(ZIO.logDebug(s"Query to execute getByModelId is ${renderRead(selectAll)}")) *>
      execute(selectAll.to((Vat.apply _).tupled))
        .provideDriver(driverLayer)
  }

}

object VatRepositoryImpl {
  val live: ZLayer[ConnectionPool, Throwable, VatRepository] =
    ZLayer.fromFunction(new VatRepositoryImpl(_))
}
